package com.example.lumaassistant.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lumaassistant.ai.AiClient
import com.example.lumaassistant.ai.SecurePrefs
import com.example.lumaassistant.automation.AutomationAccessibilityService
import com.example.lumaassistant.automation.CameraCaptureManager
import com.example.lumaassistant.automation.FlashlightController
import com.example.lumaassistant.automation.SystemControlManager
import com.example.lumaassistant.data.NoteEntity
import com.example.lumaassistant.features.Command
import com.example.lumaassistant.features.CommandRouter
import com.example.lumaassistant.features.NotesRepository
import com.example.lumaassistant.features.ReminderScheduler
import com.example.lumaassistant.voice.SpeechRecognizerManager
import com.example.lumaassistant.voice.TextToSpeechManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted

data class ChatMessage(val role: String, val text: String)

data class UiState(
    val isListening: Boolean = false,
    val isThinking: Boolean = false,
    val messages: List<ChatMessage> = emptyList(),
    val statusText: String = "Tap the mic and talk"
)

class AssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val notesRepo = NotesRepository(application)
    private val tts = TextToSpeechManager(application)
    private var speechRecognizer: SpeechRecognizerManager? = null
    private val flashlight = FlashlightController(application)
    private val systemControl = SystemControlManager(application)
    private val camera = CameraCaptureManager(application)

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val notes: StateFlow<List<NoteEntity>> = notesRepo.observeNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var reminderRequestCode = 1000

    fun onMicTapped() {
        val context = getApplication<Application>()
        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizerManager(
                context = context,
                onResult = { text -> handleUserInput(text) },
                onError = { err -> _uiState.value = _uiState.value.copy(statusText = err) },
                onListeningChange = { listening ->
                    _uiState.value = _uiState.value.copy(isListening = listening)
                }
            )
        }
        speechRecognizer?.startListening()
    }

    fun onTextSubmitted(text: String) {
        if (text.isNotBlank()) handleUserInput(text)
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch { notesRepo.deleteNote(note) }
    }

    private fun handleUserInput(text: String) {
        addMessage("user", text)
        when (val command = CommandRouter.parse(text)) {
            is Command.SetReminder -> {
                ReminderScheduler.schedule(
                    getApplication(),
                    command.triggerAtMillis,
                    command.message,
                    reminderRequestCode++
                )
                respond("Reminder set: ${command.message}")
            }
            is Command.AddNote -> {
                viewModelScope.launch {
                    notesRepo.addNote(command.text)
                    respond("Noted: ${command.text}")
                }
            }
            is Command.OpenApp -> {
                val ok = AutomationAccessibilityService.instance?.openApp(command.appName) ?: false
                respond(if (ok) "Opening ${command.appName}" else "I need Accessibility Service enabled in Settings to open apps by voice, or I couldn't find ${command.appName}.")
            }
            is Command.TypeText -> {
                val ok = AutomationAccessibilityService.instance?.typeIntoFocusedField(command.text) ?: false
                respond(if (ok) "Typed it." else "I need Accessibility Service enabled, and a text field focused on screen, to type for you.")
            }
            is Command.TapOnScreen -> {
                val ok = AutomationAccessibilityService.instance?.tapByText(command.label) ?: false
                respond(if (ok) "Tapped ${command.label}." else "I couldn't find \"${command.label}\" on screen, or Accessibility Service isn't enabled.")
            }
            is Command.Scroll -> {
                val ok = AutomationAccessibilityService.instance?.scroll(command.down) ?: false
                respond(if (ok) "Scrolled." else "I need Accessibility Service enabled to scroll for you.")
            }
            is Command.SetFlashlight -> {
                val ok = flashlight.setTorch(command.on)
                respond(if (ok) "Flashlight ${if (command.on) "on" else "off"}." else "Couldn't reach the flashlight.")
            }
            is Command.SetVolume -> {
                systemControl.setVolume(command.percent)
                respond("Volume set to ${command.percent}%.")
            }
            Command.TakeSelfie -> {
                camera.captureStill(useFrontCamera = true) { file ->
                    respond(if (file != null) "Saved a photo to ${file.name}." else "Couldn't take the photo - check Camera permission.")
                }
            }
            Command.AnalyzeScreen -> {
                val provider = SecurePrefs.getSelectedProvider(getApplication())
                val apiKey = SecurePrefs.getApiKey(getApplication(), provider)
                if (apiKey.isNullOrBlank()) {
                    respond("Add your AI API key in Settings first.")
                } else {
                    respond("Screen analysis needs a one-time screenshot permission prompt each time, per Android's rules - this is a good next step to wire up, not yet built in.")
                }
            }
            is Command.Chat -> sendToAi(command.text)
        }
    }

    private fun sendToAi(text: String) {
        val provider = SecurePrefs.getSelectedProvider(getApplication())
        val apiKey = SecurePrefs.getApiKey(getApplication(), provider)
        if (apiKey.isNullOrBlank()) {
            respond("Add your ${provider.displayName} API key in Settings first to chat.")
            return
        }
        _uiState.value = _uiState.value.copy(isThinking = true)
        viewModelScope.launch {
            val history = _uiState.value.messages.map { it.role to it.text } + ("user" to text)
            val result = AiClient(provider, apiKey).sendMessage(history)
            _uiState.value = _uiState.value.copy(isThinking = false)
            result.onSuccess { reply -> respond(reply) }
                .onFailure { err -> respond("Something went wrong: ${err.message}") }
        }
    }

    private fun respond(text: String) {
        addMessage("assistant", text)
        tts.speak(text)
    }

    private fun addMessage(role: String, text: String) {
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + ChatMessage(role, text)
        )
    }

    override fun onCleared() {
        speechRecognizer?.destroy()
        tts.shutdown()
        super.onCleared()
    }
}
