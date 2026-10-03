package com.example.lumaassistant.features

import android.content.Context
import com.example.lumaassistant.data.AppDatabase
import com.example.lumaassistant.data.NoteEntity
import kotlinx.coroutines.flow.Flow

class NotesRepository(context: Context) {
    private val dao = AppDatabase.get(context).noteDao()

    fun observeNotes(): Flow<List<NoteEntity>> = dao.observeAll()

    suspend fun addNote(text: String) {
        dao.insert(NoteEntity(text = text))
    }

    suspend fun deleteNote(note: NoteEntity) {
        dao.delete(note)
    }
}
