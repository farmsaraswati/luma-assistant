package com.example.lumaassistant.automation

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.provider.Settings

/**
 * Since Android 10, apps can no longer silently flip Wi-Fi/Bluetooth on or
 * off in the background - Google removed that specifically because it was
 * abused. The correct, honest way to do it now is to open the system's own
 * quick-settings panel, which still requires you to tap the toggle
 * yourself. Volume and brightness (with the user-granted permission) can
 * still be changed directly.
 */
class SystemControlManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun setVolume(percent: Int) {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val level = (max * percent.coerceIn(0, 100) / 100)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, level, 0)
    }

    fun openWifiPanel() {
        context.startActivity(
            Intent(Settings.Panel.ACTION_WIFI).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun openBluetoothPanel() {
        context.startActivity(
            Intent(Settings.Panel.ACTION_NFC).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun openBrightnessPanel() {
        context.startActivity(
            Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
