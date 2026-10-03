package com.example.lumaassistant.security

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.lumaassistant.R
import com.example.lumaassistant.automation.CameraCaptureManager

/**
 * Anti-Theft Guard. Deliberately scoped to only two device-admin policies
 * (see device_admin_receiver.xml): force-lock and watch-login. It does NOT
 * request wipe-data or reset-password policies, so this app can never
 * factory-reset or lock you out of your own phone - it can only lock the
 * screen (something you can already do yourself) and notice failed unlock
 * attempts.
 *
 * This is OFF by default. Turning it on always goes through Android's own
 * "Activate device admin app?" system dialog, which names this app
 * explicitly and can be revoked at any time from Settings > Security >
 * Device admin apps - turning it off there does not require uninstalling
 * the app first.
 */
class AntiTheftAdminReceiver : DeviceAdminReceiver() {

    companion object {
        private const val PREFS = "anti_theft_prefs"
        private const val KEY_ENABLED = "guard_enabled"
        private const val KEY_FAILED_COUNT = "failed_attempts"
        private const val FAILURE_THRESHOLD = 3
        private const val CHANNEL_ID = "luma_security"

        fun isEnabled(context: Context): Boolean =
            prefs(context).getBoolean(KEY_ENABLED, false)

        fun setEnabled(context: Context, enabled: Boolean) {
            prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
        }

        private fun prefs(context: Context): SharedPreferences =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    override fun onEnabled(context: Context, intent: Intent) {
        setEnabled(context, true)
    }

    override fun onDisabled(context: Context, intent: Intent) {
        setEnabled(context, false)
    }

    override fun onPasswordFailed(context: Context, intent: Intent) {
        if (!isEnabled(context)) return

        val prefs = prefs(context)
        val count = prefs.getInt(KEY_FAILED_COUNT, 0) + 1
        prefs.edit().putInt(KEY_FAILED_COUNT, count).apply()

        if (count >= FAILURE_THRESHOLD) {
            takeIntruderSelfie(context)
            notifyOwner(context, count)
        }
    }

    override fun onPasswordSucceeded(context: Context, intent: Intent) {
        prefs(context).edit().putInt(KEY_FAILED_COUNT, 0).apply()
    }

    private fun takeIntruderSelfie(context: Context) {
        CameraCaptureManager(context).captureStill(useFrontCamera = true) { file ->
            if (file != null) {
                notifyIntruderCaptured(context, file.absolutePath)
            }
        }
    }

    private fun notifyOwner(context: Context, failedAttempts: Int) {
        ensureChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Luma Anti-Theft Guard")
            .setContentText("$failedAttempts failed unlock attempts detected.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(9001, notification)
    }

    private fun notifyIntruderCaptured(context: Context, path: String) {
        ensureChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Luma Anti-Theft Guard")
            .setContentText("A photo was captured and saved on this device: $path")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(9002, notification)
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Security", NotificationManager.IMPORTANCE_HIGH
            )
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }
}
