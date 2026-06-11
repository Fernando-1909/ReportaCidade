package com.example.reportacidade

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.example.reportacidade.data.repository.AuthRepository
import com.example.reportacidade.data.repository.MockAuthRepositoryImpl
import com.example.reportacidade.data.repository.MockReportRepositoryImpl
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        //LINHAS PARA LIMPAR O BANCO DE DADOS
        //MockAuthRepositoryImpl.getInstance(this).clearAllData()
        //MockReportRepositoryImpl.getInstance(this).clearAllReports()

        authRepository = MockAuthRepositoryImpl.getInstance(this)

        val editEmail = findViewById<TextInputEditText>(R.id.editTextEmail)
        val editPassword = findViewById<TextInputEditText>(R.id.editTextPassword)
        val btnLogin = findViewById<Button>(R.id.buttonLogin)
        
        val btnForgotPassword = findViewById<TextView>(R.id.buttonForgotPassword)
        val btnGoToSignup = findViewById<TextView>(R.id.buttonGoToSignup)

        btnLogin.setOnClickListener {
            val email = editEmail.text.toString()
            val password = editPassword.text.toString()

            if (email.isBlank() || password.isBlank()) {
                Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val result = authRepository.signIn(email, password)
                if (result.isSuccess) {
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this@LoginActivity, "Erro: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        btnForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        btnGoToSignup.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
    }
}
