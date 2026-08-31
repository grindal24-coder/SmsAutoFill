package com.example.smsautofill

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

/**
 * Всплывает поверх текущего экрана (например поверх мессенджера Y) и
 * предлагает вставить найденный в SMS код — аналог системного диалога
 * SMS User Consent, но собственный и работающий без Google Play Services.
 *
 * Реально "напечатать" код в поле чужого приложения нельзя — Android не
 * позволяет одному приложению трогать UI другого. Поэтому кнопка
 * "Вставить" копирует код в буфер обмена: пользователю останется один тап
 * (долгое нажатие на поле → «Вставить», либо клавиатура сама предложит
 * скопированный текст). Полностью автоматическая вставка без единого
 * нажатия возможна только через Accessibility Service — это отдельная,
 * более тяжёлая доработка.
 */
class CodePopupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val code = intent.getStringExtra(EXTRA_CODE)
        if (code.isNullOrBlank()) {
            finish()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Обнаружен код подтверждения")
            .setMessage("Код: $code\n\nВставить его в текущее поле ввода?")
            .setPositiveButton("Вставить") { _, _ ->
                copyToClipboard(code)
                Toast.makeText(
                    this,
                    "Код скопирован — вставьте его в поле ввода",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
            .setNegativeButton("Отклонить") { _, _ -> finish() }
            .setOnDismissListener { finish() }
            .setCancelable(true)
            .show()
    }

    private fun copyToClipboard(code: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("sms_code", code))
    }

    companion object {
        const val EXTRA_CODE = "extra_code"
    }
}
