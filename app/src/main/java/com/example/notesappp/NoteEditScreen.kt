package com.example.notesappp

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.notesappp.data.Note

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditScreen(
    vm: NotesViewModel,
    noteId: Int,                       // -1 means new note
    onBack: () -> Unit,
    onSaved: (message: String) -> Unit
) {
    val context = LocalContext.current
    val isEdit = noteId != -1
    val categories by vm.categories.collectAsState()

    var existing by remember { mutableStateOf<Note?>(null) }
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Other") }
    var isPinned by remember { mutableStateOf(false) }
    var colorIndex by remember { mutableStateOf(0) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(noteId) {
        if (isEdit) {
            vm.getNote(noteId)?.let {
                existing = it
                title = it.title
                content = it.content
                category = it.category
                isPinned = it.isPinned
                colorIndex = it.colorIndex
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "Update Note" else "Add Note") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    IconButton(onClick = { isPinned = !isPinned }) {
                        Icon(
                            imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (isPinned) "Unpin" else "Pin",
                            tint = if (isPinned) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (isEdit) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, "Delete")
                        }
                    }
                    IconButton(onClick = {
                        if (title.isBlank() && content.isBlank()) {
                            Toast.makeText(context, "Note is empty", Toast.LENGTH_SHORT).show()
                        } else {
                            vm.save(
                                (existing ?: Note(title = "", content = ""))
                                    .copy(
                                        title = title.trim(),
                                        content = content.trim(),
                                        category = category,
                                        isPinned = isPinned,
                                        colorIndex = colorIndex
                                    )
                            )
                            onSaved(if (isEdit) "Note Updated Successfully" else "Note Added Successfully")
                        }
                    }) {
                        Icon(Icons.Default.Check, "Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Category chips (from the database, so your custom ones show up)
            LazyRow(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(cat) }
                    )
                }
            }

            // Color picker
            Row(
                modifier = Modifier.padding(top = 12.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                for (i in 0 until NOTE_COLOR_OPTIONS) {
                    val selected = colorIndex == i
                    Box(
                        Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(noteContainerColor(i))
                            .border(
                                width = if (selected) 3.dp else 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline,
                                shape = CircleShape
                            )
                            .clickable { colorIndex = i }
                    )
                }
            }

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Content") },
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Current Note") },
            text = { Text("Are You Sure You want to delete this note?") },
            confirmButton = {
                TextButton(onClick = {
                    existing?.let { vm.delete(it) }
                    showDeleteDialog = false
                    onSaved("Note Deleted")
                }) { Text("Yes") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("No") }
            }
        )
    }
}