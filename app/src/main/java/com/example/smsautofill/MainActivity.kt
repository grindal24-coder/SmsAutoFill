package com.example.smsautofill

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.smsautofill.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var smsHelper: SmsUserConsentHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        smsHelper = SmsUserConsentHelper(this) { code ->
            runOnUiThread {
                binding.codeEditText.setText(code)
                binding.statusText.text = "Код получен автоматически: $code"
            }
        }

        // Слушатель включается сам при открытии экрана — отдельной
        // кнопки "включить прослушивание" пользователю нажимать не нужно.
        smsHelper.startListening()

        binding.retryButton.setOnClickListener {
            smsHelper.startListening()
            binding.statusText.text = "Ждём SMS с кодом (до 5 минут)…"
            Toast.makeText(this, "Слушаем SMS заново", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        smsHelper.unregister()
    }
}
