package com.example.smsautofill

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.provider.Telephony

/**
 * Регистрируется в манифесте (не динамически!), поэтому Android доставляет
 * ему SMS_RECEIVED даже если процесс приложения не запущен вообще —
 * система сама поднимет процесс на время обработки broadcast.
 *
 * Google Play Services тут не участвуют, поэтому это работает и на
 * устройствах/прошивках без GMS.
 */
class SmsReceiver : BroadcastReceiver() {

    // Настройте под нужный формат кода
    private val codeRegex = Regex("\\b\\d{4,6}\\b")

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val fullBody = messages.joinToString(separator = "") { it.messageBody ?: "" }

        val code = codeRegex.find(fullBody)?.value ?: return

        val design = PopupPrefs.getDesign(context)

        if (design == PopupPrefs.DESIGN_OVERLAY && Settings.canDrawOverlays(context)) {
            // Дизайн 2: настоящее оверлей-окно, не подпадает под
            // ограничения на запуск Activity из фона.
            OverlayPopupHelper.show(context, code)
        } else {
            // Дизайн 1 (или запасной вариант, если разрешение на оверлей
            // не выдано): запуск синхронно внутри onReceive() — один из
            // задокументированных случаев, когда Android разрешает старт
            // Activity из фона, но некоторые устройства/версии Android
            // всё равно могут это блокировать.
            val popupIntent = Intent(context, CodePopupActivity::class.java).apply {
                putExtra(CodePopupActivity.EXTRA_CODE, code)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            context.startActivity(popupIntent)
        }
    }
}
