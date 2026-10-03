package com.example.lumaassistant.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.scale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ChatScreen(
    state: UiState,
    onMicTapped: () -> Unit,
    onTextSubmitted: (String) -> Unit
) {
    var draft by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            if (state.messages.isEmpty()) {
                item {
                    Text(
                        text = state.statusText,
                        color = TextSecondary,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(top = 48.dp)
                    )
                }
            }
            items(state.messages) { message ->
                ChatBubble(message)
            }
            if (state.isThinking) {
                item {
                    Text("Thinking…", color = TextSecondary, fontSize = 13.sp)
                }
            }
        }

        InputRow(
            draft = draft,
            onDraftChange = { draft = it },
            isListening = state.isListening,
            onMicTapped = onMicTapped,
            onSend = {
                if (draft.isNotBlank()) {
                    onTextSubmitted(draft)
                    draft = ""
                }
            }
        )
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(
                    color = if (isUser) ChatBubbleUser else ChatBubbleAssistant,
                    shape = RoundedCornerShape(
                        topStart = 18.dp, topEnd = 18.dp,
                        bottomStart = if (isUser) 18.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 18.dp
                    )
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(text = message.text, color = TextPrimary, fontSize = 15.sp, lineHeight = 20.sp)
        }
    }
}

@Composable
private fun InputRow(
    draft: String,
    onDraftChange: (String) -> Unit,
    isListening: Boolean,
    onMicTapped: () -> Unit,
    onSend: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "mic-pulse")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "pulse"
    )
    val micScale by animateFloatAsState(if (isListening) pulse else 1f, label = "mic-scale")

    Surface(color = MidnightSurface, tonalElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type or tap the mic…", color = TextSecondary) },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = Amber,
                    unfocusedBorderColor = MidnightSurfaceRaised
                )
            )
            FilledIconButton(
                onClick = onMicTapped,
                modifier = Modifier
                    .size(48.dp)
                    .scale(micScale),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (isListening) Amber else MidnightSurfaceRaised
                )
            ) {
                Icon(
                    Icons.Filled.Mic,
                    contentDescription = "Listen",
                    tint = if (isListening) MidnightBase else Amber
                )
            }
            IconButton(onClick = onSend) {
                Icon(Icons.Filled.Send, contentDescription = "Send", tint = Amber)
            }
        }
    }
}
