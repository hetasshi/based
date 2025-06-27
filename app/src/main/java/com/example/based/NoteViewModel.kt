package com.example.based

import android.app.Application
import androidx.lifecycle.*
import com.example.based.data.Note
import com.example.based.data.NoteDatabase
import kotlinx.coroutines.launch

class NoteViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = NoteDatabase.get(app).noteDao()

    val notes = dao.getAll().asLiveData()

    fun add(text: String) = viewModelScope.launch {
        if (text.isNotBlank()) dao.insert(Note(text = text.trim()))
    }

    fun delete(note: Note) = viewModelScope.launch {
        dao.delete(note)
    }
}
