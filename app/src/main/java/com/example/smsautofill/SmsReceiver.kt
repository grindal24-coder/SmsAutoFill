package com.example.smsautofill

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

/**
 * Регистрируется в манифесте (не динамически!), поэтому Android доставляет
 * ему SMS_RECEIVED даже если процесс приложения не запущен вообще —
 * система сама поднимет процесс на время обработки broadcast.
 *
 * Google Play Services тут не участвуют, поэтому это работает и на
 * устройствах/прошивках без GMS (в отличие от SMS User Consent API).
 */
class SmsReceiver : BroadcastReceiver() {

    // Настройте под нужный формат кода
    private val codeRegex = Regex("\\b\\d{4,6}\\b")

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val fullBody = messages.joinToString(separator = "") { it.messageBody ?: "" }

        val code = codeRegex.find(fullBody)?.value ?: return

        // КЛЮЧЕВОЙ МОМЕНТ: Activity запускается синхронно прямо внутри
        // onReceive(). Это одно из официально задокументированных
        // исключений из ограничений на запуск экрана из фона в Android
        // ("Restrictions on starting activities from the background" —
        // явно упоминается получение SMS_RECEIVED как валидный случай).
        // Именно поэтому попап всплывает поверх ЛЮБОГО текущего приложения
        // (в том числе Y), даже если наше приложение полностью закрыто.
        val popupIntent = Intent(context, CodePopupActivity::class.java).apply {
            putExtra(CodePopupActivity.EXTRA_CODE, code)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        context.startActivity(popupIntent)
    }
}
