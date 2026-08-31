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
 * Экран нужен только чтобы один раз выдать разрешения. Дальше вся логика
 * (SmsReceiver + CodePopupActivity) работает сама в фоне — это приложение
 * можно закрыть, попап всё равно будет всплывать при приходе SMS с кодом.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val permissionLauncher = registerForActivityResult(
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
            permissionLauncher.launch(Manifest.permission.RECEIVE_SMS)
        }

        binding.grantBatteryButton.setOnClickListener {
            requestIgnoreBatteryOptimizations()
        }

        updateStatus()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
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

        binding.statusText.text = buildString {
            append(if (hasSmsPermission) "✅ Разрешение на SMS выдано" else "❌ Разрешение на SMS не выдано")
            append("\n")
            append(if (ignoringBattery) "✅ Ограничения батареи сняты" else "❌ Ограничения батареи ещё активны")
            append("\n\n")
            if (hasSmsPermission && ignoringBattery) {
                append("Всё готово. Можно закрыть приложение — фоновое обнаружение кода продолжит работать.")
            }
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
