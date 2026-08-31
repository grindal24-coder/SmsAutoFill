package com.example.smsautofill

import android.content.Context

/** Хранит, какой дизайн попапа сейчас выбран: "activity" или "overlay". */
object PopupPrefs {
    private const val PREFS = "popup_prefs"
    private const val KEY_DESIGN = "popup_design"

    const val DESIGN_ACTIVITY = "activity"
    const val DESIGN_OVERLAY = "overlay"

    fun getDesign(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_DESIGN, DESIGN_OVERLAY) ?: DESIGN_OVERLAY

    fun setDesign(context: Context, design: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_DESIGN, design)
            .apply()
    }
}
