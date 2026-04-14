package com.example.reday.utils

import android.content.Context

object TokenManager {
    private const val PREFS_NAME = "auth_prefs"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_EMAIL = "user_email"

    fun saveToken(context: Context, token: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ACCESS_TOKEN, token)
            .apply()
    }

    fun getToken(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ACCESS_TOKEN, null)
    }

    fun saveUserId(context: Context, userId: Long) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_USER_ID, userId)
            .apply()
    }

    fun getUserId(context: Context): Long? {
        val id = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_USER_ID, -1L)
        return if (id == -1L) null else id
    }

    fun saveUserName(context: Context, name: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_USER_NAME, name).apply()
    }

    fun getUserName(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_USER_NAME, "") ?: ""
    }

    fun saveUserEmail(context: Context, email: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_USER_EMAIL, email).apply()
    }

    fun getUserEmail(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_USER_EMAIL, "") ?: ""
    }

    fun clearToken(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_USER_ID)
            .remove(KEY_USER_NAME)
            .remove(KEY_USER_EMAIL)
            .apply()
    }
}
