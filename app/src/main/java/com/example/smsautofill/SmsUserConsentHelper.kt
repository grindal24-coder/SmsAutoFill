package com.example.smsautofill

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status

/**
 * Обёртка над Android SMS User Consent API.
 *
 * Как это работает "под капотом":
 *  1. startListening() говорит Google Play Services: "начни слушать входящие
 *     SMS в течение следующих 5 минут".
 *  2. Как только приходит любая SMS, Play Services присылают broadcast
 *     SmsRetriever.SMS_RETRIEVED_ACTION со статусом SUCCESS и Intent-ом,
 *     который при запуске показывает СИСТЕМНЫЙ диалог "Разрешить приложению
 *     ... прочитать сообщение и ввести код?" (тот самый со скриншота).
 *  3. Если пользователь нажимает "Разрешить" — в onActivityResult(...)
 *     прилетает полный текст SMS, из которого мы регуляркой вытаскиваем код.
 *  4. Если за 5 минут ничего не пришло — статус TIMEOUT, слушатель "гаснет",
 *     и его нужно запускать заново (кнопка retry в демо-экране).
 *
 * ВАЖНО: Google Play Services не даёт "вечно" слушать SMS в фоне без
 * подтверждения. Явный вызов startListening() обязателен — это сделано
 * намеренно (батарея/приватность). Что можно сделать — вызывать
 * startListening() автоматически в нужный момент (например в onCreate/
 * onResume экрана ввода кода), тогда с точки зрения пользователя всё
 * происходит "само".
 */
class SmsUserConsentHelper(
    private val activity: AppCompatActivity,
    // Регулярка под формат вашего кода. Тут — 4-6 цифр подряд.
    private val codeRegex: Regex = Regex("\\b\\d{4,6}\\b"),
    private val onCodeReceived: (String) -> Unit
) {

    private var isRegistered = false

    private val consentLauncher: ActivityResultLauncher<Intent> =
        activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val message: String? = result.data
                    ?.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE)
                message?.let { extractAndDeliverCode(it) }
            }
            // RESULT_CANCELED — пользователь нажал "Отклонить"
        }

    private val smsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != SmsRetriever.SMS_RETRIEVED_ACTION) return

            val extras = intent.extras ?: return
            val status = extras.get(SmsRetriever.EXTRA_STATUS) as? Status ?: return

            when (status.statusCode) {
                CommonStatusCodes.SUCCESS -> {
                    val consentIntent =
                        extras.getParcelable<Intent>(SmsRetriever.EXTRA_CONSENT_INTENT)
                    consentIntent?.let {
                        try {
                            consentLauncher.launch(it)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                CommonStatusCodes.TIMEOUT -> {
                    // Окно ожидания (5 минут) истекло без входящей SMS
                }
            }
        }
    }

    /** Включить прослушивание SMS на ближайшие 5 минут. */
    fun startListening() {
        register()
        val client = SmsRetriever.getClient(activity)
        client.startSmsUserConsent(null) // null = любой отправитель
    }

    private fun register() {
        if (isRegistered) return
        val filter = IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.registerReceiver(smsReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            activity.registerReceiver(smsReceiver, filter)
        }
        isRegistered = true
    }

    /** Отписать ресивер, вызывать в onDestroy(). */
    fun unregister() {
        if (!isRegistered) return
        try {
            activity.unregisterReceiver(smsReceiver)
        } catch (e: IllegalArgumentException) {
            // уже отписан
        }
        isRegistered = false
    }

    private fun extractAndDeliverCode(smsBody: String) {
        codeRegex.find(smsBody)?.value?.let(onCodeReceived)
    }
}
