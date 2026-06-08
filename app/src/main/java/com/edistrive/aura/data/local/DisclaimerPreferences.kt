package com.edistrive.aura.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DisclaimerPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "disclaimer_prefs",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_HAS_ACCEPTED = "hasAcceptedDisclaimer"
    }

    fun hasAcceptedDisclaimer(): Boolean = prefs.getBoolean(KEY_HAS_ACCEPTED, false)

    fun setAcceptedDisclaimer(accepted: Boolean) {
        prefs.edit().putBoolean(KEY_HAS_ACCEPTED, accepted).apply()
    }
}
