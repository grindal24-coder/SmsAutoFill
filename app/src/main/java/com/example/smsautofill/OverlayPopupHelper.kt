package com.example.smsautofill

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast

/**
 * Дизайн 2: настоящее оверлей-окно через WindowManager (не Activity!).
 * Требует разрешение SYSTEM_ALERT_WINDOW ("Отображать поверх других
 * приложений"). Такие окна НЕ подпадают под ограничения Android на запуск
 * Activity из фона — поэтому появляются поверх любого приложения (Y)
 * надёжно, даже когда наше приложение полностью закрыто и не в фокусе.
 */
object OverlayPopupHelper {

    private var currentView: android.view.View? = null

    fun show(context: Context, code: String) {
        val appContext = context.applicationContext
        val windowManager = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        // Если предыдущий оверлей ещё не убран — уберём его сначала
        dismiss(appContext)

        val view = LayoutInflater.from(appContext).inflate(R.layout.overlay_code_popup, null)
        view.findViewById<TextView>(R.id.overlayCodeText).text = "Найден код: $code"

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP
        params.y = 80

        view.findViewById<Button>(R.id.overlayInsertButton).setOnClickListener {
            val clipboard = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("sms_code", code))
            Toast.makeText(appContext, "Код скопирован — вставьте его в поле ввода", Toast.LENGTH_LONG).show()
            dismiss(appContext)
        }
        view.findViewById<Button>(R.id.overlayDismissButton).setOnClickListener {
            dismiss(appContext)
        }

        windowManager.addView(view, params)
        currentView = view

        // Автоскрытие через 20 секунд, если пользователь не отреагировал
        Handler(Looper.getMainLooper()).postDelayed({ dismiss(appContext) }, 20_000)
    }

    private fun dismiss(context: Context) {
        val view = currentView ?: return
        try {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager.removeView(view)
        } catch (e: Exception) {
            // окно уже убрано — игнорируем
        }
        currentView = null
    }
}
