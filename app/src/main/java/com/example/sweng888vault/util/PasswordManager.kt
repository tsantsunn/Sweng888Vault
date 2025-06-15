package com.example.sweng888vault.util

import android.content.Context

object PasswordManager { // Use 'object' for a singleton utility

    private const val PREFS_NAME = "AppPrefs"
    private const val KEY_PASSWORD_HASH = "app_password_hash"
    private const val KEY_PASSWORD_SET = "password_set"

    // This is still an INSECURE placeholder. REPLACE WITH REAL HASHING.
    private fun hashPassword(password: String): String {
        return "HASHED_" + password.hashCode().toString()
    }

    fun isPasswordSet(context: Context): Boolean {
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPref.getBoolean(KEY_PASSWORD_SET, false)
    }

    fun setPassword(context: Context, password: String) {
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString(KEY_PASSWORD_HASH, hashPassword(password))
            putBoolean(KEY_PASSWORD_SET, true)
            apply()
        }
    }

    fun verifyPassword(context: Context, enteredPassword: String): Boolean {
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val storedHashedPassword = sharedPref.getString(KEY_PASSWORD_HASH, null)
        return storedHashedPassword == hashPassword(enteredPassword)
    }
}