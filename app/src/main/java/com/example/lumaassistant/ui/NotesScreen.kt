package com.example.lumaassistant.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lumaassistant.data.NoteEntity

@Composable
fun NotesScreen(notes: List<NoteEntity>, onDelete: (NoteEntity) -> Unit) {
    if (notes.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text(
                "Say \"note that…\" or add one here.",
                color = TextSecondary
            )
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(notes) { note ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MidnightSurfaceRaised, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(note.text, color = TextPrimary, fontSize = 15.sp, modifier = Modifier.weight(1f))
                IconButton(onClick = { onDelete(note) }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = TextSecondary)
                }
            }
        }
    }
}
