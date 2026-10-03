package com.example.lumaassistant

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lumaassistant.ui.*

class MainActivity : ComponentActivity() {

    private val viewModel: AssistantViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* results handled implicitly; mic button will no-op without RECORD_AUDIO */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissions.toTypedArray())

        setContent {
            LumaAssistantTheme {
                LumaApp(viewModel)
            }
        }
    }
}

private enum class Tab(val label: String) { CHAT("Chat"), NOTES("Notes"), SETTINGS("Settings") }

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun LumaApp(viewModel: AssistantViewModel) {
    var tab by remember { mutableStateOf(Tab.CHAT) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MidnightBase,
        topBar = {
            TopAppBar(
                title = { Text("Luma") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MidnightBase,
                    titleContentColor = TextPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MidnightSurface) {
                NavigationBarItem(
                    selected = tab == Tab.CHAT,
                    onClick = { tab = Tab.CHAT },
                    icon = { Icon(Icons.Filled.Chat, contentDescription = null) },
                    label = { Text("Chat") }
                )
                NavigationBarItem(
                    selected = tab == Tab.NOTES,
                    onClick = { tab = Tab.NOTES },
                    icon = { Icon(Icons.Filled.EditNote, contentDescription = null) },
                    label = { Text("Notes") }
                )
                NavigationBarItem(
                    selected = tab == Tab.SETTINGS,
                    onClick = { tab = Tab.SETTINGS },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = { Text("Settings") }
                )
            }
        }
    ) { padding ->
        Surface(
            modifier = androidx.compose.ui.Modifier.padding(padding),
            color = MidnightBase
        ) {
            when (tab) {
                Tab.CHAT -> ChatScreen(
                    state = uiState,
                    onMicTapped = viewModel::onMicTapped,
                    onTextSubmitted = viewModel::onTextSubmitted
                )
                Tab.NOTES -> NotesScreen(
                    notes = notes,
                    onDelete = viewModel::deleteNote
                )
                Tab.SETTINGS -> SettingsScreen()
            }
        }
    }
}
