package com.example.notesappp

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.notesappp.data.Note
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: NotesViewModel,
    snackbarHostState: SnackbarHostState,
    onAddNote: () -> Unit,
    onOpenNote: (Int) -> Unit
) {
    val notesState by vm.notes.collectAsState()
    val query by vm.query.collectAsState()
    val selectedCategory by vm.selectedCategory.collectAsState()
    val categories by vm.categories.collectAsState()
    val sortOrder by vm.sortOrder.collectAsState()
    val dark by vm.darkMode.collectAsState()
    val list = notesState

    val scope = rememberCoroutineScope()
    var showSortMenu by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddNote,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add note")
            }
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(horizontal = 12.dp)
                .fillMaxSize()
        ) {
            // Search + dark mode toggle
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = vm::setQuery,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Search Here...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { vm.setQuery("") }) {
                                Icon(Icons.Default.Close, "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small
                )
                IconButton(onClick = { vm.setDarkMode(!dark) }) {
                    Icon(
                        imageVector = if (dark) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Toggle dark mode"
                    )
                }
            }

            // Category chips + sort menu
            Row(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { vm.setCategory(null) },
                            label = { Text("All") }
                        )
                    }
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { vm.setCategory(if (selectedCategory == cat) null else cat) },
                            label = { Text(cat) }
                        )
                    }
                    item {
                        AssistChip(
                            onClick = { showCategoryDialog = true },
                            label = { Text("Edit") },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, null, Modifier.size(18.dp))
                            }
                        )
                    }
                }
                Box {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        SortOrder.entries.forEach { order ->
                            DropdownMenuItem(
                                text = { Text(order.label) },
                                onClick = {
                                    vm.setSort(order)
                                    showSortMenu = false
                                },
                                leadingIcon = {
                                    if (order == sortOrder) Icon(Icons.Default.Check, null)
                                }
                            )
                        }
                    }
                }
            }

            when {
                // 1. Loading
                list == null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                // 2. No notes at all
                list.isEmpty() && query.isBlank() && selectedCategory == null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Image(
                                painter = painterResource(R.drawable.empty_note),
                                contentDescription = null,
                                modifier = Modifier.size(260.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text("No notes yet", fontWeight = FontWeight.Bold)
                            Text(
                                "Tap + to add your first note",
                                fontSize = 13.sp,
                                modifier = Modifier.alpha(0.6f)
                            )
                        }
                    }
                }

                // 3. Nothing matches the filter / search
                list.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            if (query.isNotBlank()) "No notes match \"$query\""
                            else "No notes in $selectedCategory",
                            modifier = Modifier.alpha(0.6f)
                        )
                    }
                }

                // 4. Notes available
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 88.dp)
                    ) {
                        items(list, key = { it.id }) { note ->
                            val currentNote by rememberUpdatedState(note)

                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { value ->
                                    if (value == SwipeToDismissBoxValue.EndToStart) {
                                        val deleted = currentNote
                                        vm.delete(deleted)
                                        scope.launch {
                                            snackbarHostState.currentSnackbarData?.dismiss()
                                            val result = snackbarHostState.showSnackbar(
                                                message = "Note deleted",
                                                actionLabel = "Undo",
                                                duration = SnackbarDuration.Short
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                vm.restore(deleted)
                                            }
                                        }
                                        true
                                    } else {
                                        false
                                    }
                                }
                            )

                            SwipeToDismissBox(
                                state = dismissState,
                                enableDismissFromStartToEnd = false,
                                backgroundContent = {
                                    Box(
                                        Modifier
                                            .fillMaxSize()
                                            .clip(MaterialTheme.shapes.medium)
                                            .background(MaterialTheme.colorScheme.errorContainer)
                                            .padding(end = 20.dp),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            ) {
                                NoteItem(
                                    note = note,
                                    onClick = { onOpenNote(note.id) },
                                    onTogglePin = { vm.togglePin(note) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCategoryDialog) {
        CategoriesDialog(
            categories = categories,
            onAdd = { vm.addCategory(it) },
            onDelete = { vm.deleteCategory(it) },
            onDismiss = { showCategoryDialog = false }
        )
    }
}

@Composable
fun NoteItem(note: Note, onClick: () -> Unit, onTogglePin: () -> Unit) {
    val date = remember(note.updatedAt) {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(note.updatedAt))
    }
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = noteContainerColor(note.colorIndex))
    ) {
        Row(
            Modifier.padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    note.category,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    note.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    note.content,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Updated At: $date",
                    fontSize = 10.sp,
                    modifier = Modifier.alpha(0.6f)
                )
            }
            IconButton(onClick = onTogglePin) {
                Icon(
                    imageVector = if (note.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                    contentDescription = if (note.isPinned) "Unpin" else "Pin",
                    tint = if (note.isPinned) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
fun CategoriesDialog(
    categories: List<String>,
    onAdd: (String) -> String?,      // returns an error message or null
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Categories") },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val e = error
                    OutlinedTextField(
                        value = text,
                        onValueChange = {
                            text = it.take(20)
                            error = null
                        },
                        label = { Text("New category") },
                        singleLine = true,
                        isError = e != null,
                        supportingText = if (e != null) { { Text(e) } } else null,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        val result = onAdd(text)
                        if (result == null) text = "" else error = result
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Add category")
                    }
                }
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.heightIn(max = 240.dp)) {
                    items(categories) { cat ->
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(cat, Modifier.weight(1f))
                            // "Other" is the fallback, so it can't be deleted
                            if (cat != "Other") {
                                IconButton(onClick = { onDelete(cat) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete $cat")
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}