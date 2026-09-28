package com.mountaincrab.crabdo.notification

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.getSystemService

/**
 * Helpers for permissions that can't be granted through a runtime dialog and
 * instead need the user to flip a switch in system settings.
 */
object AppPermissions {

    /**
     * Whether alarm notifications are allowed to launch full-screen over the lock
     * screen. On Android 14+ `USE_FULL_SCREEN_INTENT` is a special app access that
     * the Play Store / system may revoke at install time, so it must be checked.
     */
    fun canUseFullScreenIntent(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            context.getSystemService<NotificationManager>()?.canUseFullScreenIntent() ?: false
        } else true

    /** Opens the "Full screen notifications" toggle for this app (falls back to app details). */
    fun openFullScreenIntentSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT)
                .setData(packageUri(context))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (runCatching { context.startActivity(intent) }.isSuccess) return
        }
        openAppSettings(context)
    }

    /** Opens the system "App info" page for this app. */
    fun openAppSettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(packageUri(context))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private fun packageUri(context: Context): Uri = Uri.fromParts("package", context.packageName, null)
}
