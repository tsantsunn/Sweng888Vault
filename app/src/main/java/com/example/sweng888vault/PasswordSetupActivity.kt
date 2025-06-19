package com.example.sweng888vault

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.example.sweng888vault.util.PasswordManager // Import the new utility

class PasswordSetupActivity : AppCompatActivity() {

    private lateinit var editTextPassword: TextInputEditText
    private lateinit var editTextConfirmPassword: TextInputEditText
    private lateinit var buttonSetPassword: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_password_setup)

        editTextPassword = findViewById(R.id.editTextPassword)
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword)
        buttonSetPassword = findViewById(R.id.buttonSetPassword)

        buttonSetPassword.setOnClickListener {
            setupPassword()
        }
    }

    private fun setupPassword() {
        val password = editTextPassword.text.toString()
        val confirmPassword = editTextConfirmPassword.text.toString()

        if (password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Password fields cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(this, "Password must be at least 6 characters long", Toast.LENGTH_SHORT).show()
            return
        }

        PasswordManager.setPassword(this, password) // Use PasswordManager

        Toast.makeText(this, "Password set successfully!", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, MainActivity::class.java) // Assuming MainActivity.kt is the final destination after setup
        startActivity(intent)
        finish()
    }

    // No need for isPasswordSet or verifyPassword methods here anymore, as PasswordManager handles them
}