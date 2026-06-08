package com.edistrive.aura.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 认证信息本地存储 - 对标 iOS 的 UserDefaults
 */
@Singleton
class AuthPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "auth_prefs",
        Context.MODE_PRIVATE
    )
    
    companion object {
        private const val KEY_TOKEN = "token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_AVATAR_URL = "avatar_url"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }
    
    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }
    
    fun getToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }
    
    fun saveUserId(userId: Int) {
        prefs.edit().putInt(KEY_USER_ID, userId).apply()
    }
    
    fun getUserId(): Int {
        return prefs.getInt(KEY_USER_ID, -1)
    }
    
    fun saveUsername(username: String) {
        prefs.edit().putString(KEY_USERNAME, username).apply()
    }
    
    fun getUsername(): String? {
        return prefs.getString(KEY_USERNAME, null)
    }

    fun saveAvatarUrl(url: String) {
        prefs.edit().putString(KEY_AVATAR_URL, url).apply()
    }

    fun getAvatarUrl(): String? {
        return prefs.getString(KEY_AVATAR_URL, null)
    }
    
    fun setLoggedIn(isLoggedIn: Boolean) {
        prefs.edit().putBoolean(KEY_IS_LOGGED_IN, isLoggedIn).apply()
    }
    
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }
    
    fun clear() {
        prefs.edit().clear().apply()
    }
}
