package com.linguaai.app.utils

import android.content.Context
import android.content.SharedPreferences
import com.linguaai.app.models.User
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

object SessionManager {
    private const val PREF_NAME = "linguaai_prefs"
    private const val KEY_USER = "current_user"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"

    private lateinit var prefs: SharedPreferences
    private val json = Json { ignoreUnknownKeys = true }

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveUser(user: User) {
        prefs.edit()
            .putString(KEY_USER, json.encodeToString(user))
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .apply()
    }

    fun getUser(): User? {
        val userJson = prefs.getString(KEY_USER, null) ?: return null
        return try {
            json.decodeFromString<User>(userJson)
        } catch (e: Exception) {
            null
        }
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    fun logout() {
        prefs.edit()
            .remove(KEY_USER)
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .apply()
    }
}
