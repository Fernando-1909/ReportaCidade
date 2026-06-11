package com.example.reportacidade

import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.reportacidade.data.repository.AuthRepository
import com.example.reportacidade.data.repository.MockAuthRepositoryImpl
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_forgot_password)

        authRepository = MockAuthRepositoryImpl.getInstance(this)

        val btnBack = findViewById<ImageButton>(R.id.buttonBack)
        val editEmail = findViewById<TextInputEditText>(R.id.editTextEmail)
        val editNewPassword = findViewById<TextInputEditText>(R.id.editTextNewPassword)
        val editConfirmPassword = findViewById<TextInputEditText>(R.id.editTextConfirmPassword)
        val btnReset = findViewById<Button>(R.id.buttonResetPassword)

        btnBack.setOnClickListener { finish() }

        btnReset.setOnClickListener {
            val email = editEmail.text.toString()
            val newPassword = editNewPassword.text.toString()
            val confirmPassword = editConfirmPassword.text.toString()

            if (email.isBlank() || newPassword.isBlank() || confirmPassword.isBlank()) {
                Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword != confirmPassword) {
                Toast.makeText(this, "As senhas não coincidem", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val result = authRepository.updatePassword(email, newPassword)
                if (result.isSuccess) {
                    Toast.makeText(this@ForgotPasswordActivity, "Senha alterada com sucesso!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@ForgotPasswordActivity, "Erro: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
