package com.example.lumaassistant.ui

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.lumaassistant.ai.AiProvider
import com.example.lumaassistant.ai.SecurePrefs
import com.example.lumaassistant.security.AntiTheftAdminReceiver
import androidx.compose.ui.platform.LocalContext

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    var provider by remember { mutableStateOf(SecurePrefs.getSelectedProvider(context)) }
    var key by remember(provider) { mutableStateOf(SecurePrefs.getApiKey(context, provider).orEmpty()) }
    var saved by remember { mutableStateOf(false) }
    var providerMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("AI Provider", color = TextPrimary, fontSize = 18.sp)
        Text(
            "Pick which AI answers your chat messages. Switch anytime - each " +
                "provider keeps its own saved key, so you can come back to one " +
                "you used before without re-entering it.",
            color = TextSecondary,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )

        Box {
            OutlinedButton(
                onClick = { providerMenuExpanded = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
            ) {
                Text(provider.displayName)
            }
            DropdownMenu(
                expanded = providerMenuExpanded,
                onDismissRequest = { providerMenuExpanded = false }
            ) {
                AiProvider.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.displayName) },
                        onClick = {
                            provider = option
                            SecurePrefs.saveSelectedProvider(context, option)
                            providerMenuExpanded = false
                            saved = false
                        }
                    )
                }
            }
        }

        Text(
            "Get a key for ${provider.displayName.substringBefore(" (")} at ${provider.getKeyUrl} (${provider.keyHint})",
            color = TextSecondary,
            fontSize = 12.sp
        )

        OutlinedTextField(
            value = key,
            onValueChange = { key = it; saved = false },
            label = { Text("API key for ${provider.displayName.substringBefore(" (")}") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = Amber,
                unfocusedBorderColor = MidnightSurfaceRaised
            )
        )

        Button(
            onClick = {
                SecurePrefs.saveApiKey(context, provider, key.trim())
                saved = true
            },
            colors = ButtonDefaults.buttonColors(containerColor = Amber, contentColor = MidnightBase)
        ) {
            Text("Save key")
        }

        if (saved) {
            Text("Saved.", color = Amber, fontSize = 13.sp)
        }

        Divider(color = MidnightSurfaceRaised)

        Text("Screen Automation", color = TextPrimary, fontSize = 18.sp)
        Text(
            "Lets Luma open apps, type text, tap buttons, and scroll when YOU " +
                "ask it to (\"open WhatsApp\", \"type hello\", \"tap send\"). This is " +
                "the same permission (Accessibility Service) that lets Luma read what's " +
                "on your screen while it's active - only enable it if you plan to use " +
                "these voice commands, and you can turn it off anytime from this same " +
                "system screen without uninstalling the app.\n\n" +
                "Note: using this to auto-send WhatsApp messages can violate WhatsApp's " +
                "Terms of Service and risks your WhatsApp account being flagged - that's " +
                "a WhatsApp policy, not something this app controls.",
            color = TextSecondary,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
        Button(
            onClick = {
                context.startActivity(
                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            },
            colors = ButtonDefaults.buttonColors(containerColor = MidnightSurfaceRaised, contentColor = Amber)
        ) {
            Text("Open Accessibility Settings")
        }

        Divider(color = MidnightSurfaceRaised)

        Text("Anti-Theft Guard", color = TextPrimary, fontSize = 18.sp)
        Text(
            "Off by default. If enabled: after 3 failed unlock attempts on this " +
                "phone, Luma takes one front-camera photo and saves it locally on " +
                "this device (never uploaded anywhere), and shows a notification. " +
                "This app cannot wipe your phone or reset your lock password - only " +
                "lock the screen and notice failed attempts. Disable anytime below.",
            color = TextSecondary,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
        var guardEnabled by remember { mutableStateOf(AntiTheftAdminReceiver.isEnabled(context)) }
        Button(
            onClick = {
                val adminComponent = ComponentName(context, AntiTheftAdminReceiver::class.java)
                val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                if (dpm.isAdminActive(adminComponent)) {
                    dpm.removeActiveAdmin(adminComponent)
                    guardEnabled = false
                } else {
                    val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                        putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                        putExtra(
                            DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                            "Lets Luma lock the screen and notice failed unlock attempts for Anti-Theft Guard."
                        )
                    }
                    context.startActivity(intent)
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (guardEnabled) MidnightSurfaceRaised else Amber,
                contentColor = if (guardEnabled) Amber else MidnightBase
            )
        ) {
            Text(if (guardEnabled) "Disable Anti-Theft Guard" else "Enable Anti-Theft Guard")
        }

        Divider(color = MidnightSurfaceRaised)

        Text("What this app can and can't do", color = TextPrimary, fontSize = 16.sp)
        listOf(
            "Listens only while you're holding the mic button open",
            "Sets reminders and notes only when you ask, by voice or text",
            "Opens apps, types, taps, or scrolls only right after you say to - never on its own",
            "Anti-Theft Guard and Screen Automation are both off until you turn them on here, and both can be turned off the same way, without uninstalling",
            "No background service keeps running or resists removal"
        ).forEach {
            Text("•  $it", color = TextSecondary, fontSize = 13.sp)
        }
    }
}
