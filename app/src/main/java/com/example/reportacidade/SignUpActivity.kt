package com.example.reportacidade

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.reportacidade.data.repository.AuthRepository
import com.example.reportacidade.data.repository.MockAuthRepositoryImpl
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.example.reportacidade.utils.LocationData
import com.example.reportacidade.utils.LocationUtils
import kotlinx.coroutines.launch

class SignUpActivity : AppCompatActivity() {

    private lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_signup)

        authRepository = MockAuthRepositoryImpl.getInstance(this)

        val btnBack = findViewById<ImageButton>(R.id.buttonBack)
        val editName = findViewById<TextInputEditText>(R.id.editTextName)
        val editEmail = findViewById<TextInputEditText>(R.id.editTextEmail)
        val autoCompleteCity = findViewById<MaterialAutoCompleteTextView>(R.id.autoCompleteCity)
        val autoCompleteNeighborhood = findViewById<MaterialAutoCompleteTextView>(R.id.autoCompleteNeighborhood)
        val editPassword = findViewById<TextInputEditText>(R.id.editTextPassword)
        val editConfirmPassword = findViewById<TextInputEditText>(R.id.editTextConfirmPassword)
        val btnSignup = findViewById<Button>(R.id.buttonSignup)
        val btnGoToLogin = findViewById<TextView>(R.id.buttonGoToLogin)

        // Configurar Cidades
        val cityAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, LocationUtils.rnCities)
        autoCompleteCity.setAdapter(cityAdapter)

        // Ouvinte para mudar bairros quando a cidade for selecionada
        autoCompleteCity.setOnItemClickListener { parent, _, position, _ ->
            val cityName = parent.getItemAtPosition(position) as String
            val neighborhoods = LocationUtils.neighborhoodMap[cityName] ?: emptyList()
            
            val neighborhoodAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, neighborhoods)
            autoCompleteNeighborhood.setText("", false) // Limpa seleção anterior sem filtrar
            autoCompleteNeighborhood.setAdapter(neighborhoodAdapter)
            
            val layoutNeighborhood = findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.layoutNeighborhood)
            if (neighborhoods.isEmpty()) {
                layoutNeighborhood.hint = "Bairros em breve..."
            } else {
                layoutNeighborhood.hint = "Selecione seu bairro"
            }
        }

        btnBack.setOnClickListener { finish() }

        btnSignup.setOnClickListener {
            val name = editName.text.toString()
            val email = editEmail.text.toString()
            val city = autoCompleteCity.text.toString()
            val neighborhood = autoCompleteNeighborhood.text.toString()
            val password = editPassword.text.toString()
            val confirmPassword = editConfirmPassword.text.toString()

            if (name.isBlank() || email.isBlank() || city.isBlank() || neighborhood.isBlank() || password.isBlank()) {
                Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "As senhas não coincidem", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val result = authRepository.signUp(name, email, password, city, neighborhood)
                if (result.isSuccess) {
                    Toast.makeText(this@SignUpActivity, "Conta criada com sucesso!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@SignUpActivity, MainActivity::class.java))
                    finishAffinity()
                } else {
                    Toast.makeText(this@SignUpActivity, "Erro: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        btnGoToLogin.setOnClickListener {
            finish()
        }
    }
}
