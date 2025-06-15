package com.example.sweng888vault

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.sweng888vault.util.PasswordManager // Import the new utility

class LauncherActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)

        val splashScreenDuration = 1000L

        android.os.Handler().postDelayed({
            if (!PasswordManager.isPasswordSet(this)) { // Use PasswordManager
                val intent = Intent(this, PasswordSetupActivity::class.java)
                startActivity(intent)
            } else {
                val intent = Intent(this, PasswordLoginActivity::class.java)
                startActivity(intent)
            }
            finish()
        }, splashScreenDuration)
    }
}