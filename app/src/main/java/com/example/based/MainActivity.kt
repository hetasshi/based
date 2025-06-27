package com.example.based

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.based.data.Note              // ← новый импорт
import com.example.based.ui.theme.BasedTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            BasedTheme {
                val vm: NoteViewModel = viewModel()
                val notes: List<Note> by vm.notes.observeAsState(emptyList())

                var input by remember { mutableStateOf("") }

                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(16.dp)
                    ) {
                        TextField(
                            value = input,
                            onValueChange = { input = it },
                            placeholder = { Text("Новая заметка") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = { vm.add(input); input = "" },
                            enabled = input.isNotBlank(),
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .align(Alignment.End)
                        ) {
                            Text("Добавить")
                        }

                        LazyColumn(
                            Modifier
                                .fillMaxSize()
                                .padding(top = 8.dp)
                        ) {
                            items(notes, key = { it.id }) { note ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        note.text,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { vm.delete(note) }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Удалить"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
