package com.example.smsautofill

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.smsautofill.databinding.ActivityMainBinding

/**
 * Экран нужен, чтобы один раз выдать разрешения и выбрать дизайн попапа.
 * Дальше вся логика (SmsReceiver + попап) работает сама в фоне — это
 * приложение можно закрыть, попап всё равно появится при приходе SMS.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val smsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        updateStatus()
        if (!granted) {
            Toast.makeText(
                this,
                "Без разрешения на SMS фоновое чтение кода работать не будет",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.grantSmsButton.setOnClickListener {
            smsPermissionLauncher.launch(Manifest.permission.RECEIVE_SMS)
        }

        binding.grantBatteryButton.setOnClickListener {
            requestIgnoreBatteryOptimizations()
        }

        binding.grantOverlayButton.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
        }

        val currentDesign = PopupPrefs.getDesign(this)
        binding.designGroup.check(
            if (currentDesign == PopupPrefs.DESIGN_OVERLAY) binding.designOverlay.id
            else binding.designActivity.id
        )
        binding.designGroup.setOnCheckedChangeListener { _, checkedId ->
            val newDesign = if (checkedId == binding.designOverlay.id)
                PopupPrefs.DESIGN_OVERLAY else PopupPrefs.DESIGN_ACTIVITY
            PopupPrefs.setDesign(this, newDesign)
        }

        binding.previewButton.setOnClickListener {
            previewCurrentDesign()
        }

        updateStatus()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun previewCurrentDesign() {
        val design = PopupPrefs.getDesign(this)
        if (design == PopupPrefs.DESIGN_OVERLAY) {
            if (Settings.canDrawOverlays(this)) {
                OverlayPopupHelper.show(this, "12345")
            } else {
                Toast.makeText(
                    this,
                    "Сначала выдайте разрешение \"поверх приложений\"",
                    Toast.LENGTH_LONG
                ).show()
            }
        } else {
            val intent = Intent(this, CodePopupActivity::class.java).apply {
                putExtra(CodePopupActivity.EXTRA_CODE, "12345")
            }
            startActivity(intent)
        }
    }

    private fun updateStatus() {
        val hasSmsPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED

        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        val ignoringBattery = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            powerManager.isIgnoringBatteryOptimizations(packageName)
        } else {
            true
        }

        val hasOverlayPermission = Settings.canDrawOverlays(this)

        binding.statusText.text = buildString {
            append(if (hasSmsPermission) "✅ Разрешение на SMS выдано" else "❌ Разрешение на SMS не выдано")
            append("\n")
            append(if (ignoringBattery) "✅ Ограничения батареи сняты" else "❌ Ограничения батареи ещё активны")
            append("\n")
            append(if (hasOverlayPermission) "✅ Разрешение \"поверх приложений\" выдано" else "❌ Разрешение \"поверх приложений\" не выдано")
        }
    }

    private fun requestIgnoreBatteryOptimizations() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            val intent = Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }
}
