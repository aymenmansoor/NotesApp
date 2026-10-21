package com.example.notesappp

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.notesappp.data.Category
import com.example.notesappp.data.Note
import com.example.notesappp.data.NoteDatabase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class SortOrder(val label: String) {
    NEWEST("Newest first"),
    OLDEST("Oldest first"),
    TITLE("Title A–Z")
}

@OptIn(ExperimentalCoroutinesApi::class)
class NotesViewModel(app: Application) : AndroidViewModel(app) {
    private val db = NoteDatabase.get(app)
    private val dao = db.noteDao()
    private val categoryDao = db.categoryDao()
    private val prefs = app.getSharedPreferences("settings", Context.MODE_PRIVATE)

    val query = MutableStateFlow("")
    val selectedCategory = MutableStateFlow<String?>(null)   // null = All
    val sortOrder = MutableStateFlow(SortOrder.NEWEST)

    // First launch follows the phone's setting, after that the saved choice wins
    val darkMode = MutableStateFlow(
        prefs.getBoolean(
            "dark_mode",
            (app.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                    Configuration.UI_MODE_NIGHT_YES
        )
    )

    val categories: StateFlow<List<String>> = categoryDao.observeAll()
        .map { list -> list.map { it.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notes: StateFlow<List<Note>?> =
        combine(query, selectedCategory, sortOrder) { q, c, s -> Triple(q.trim(), c, s) }
            .flatMapLatest { (q, c, s) -> dao.observe(q, c).map { sortNotes(it, s) } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Pinned notes always stay on top, then the chosen order applies
    private fun sortNotes(list: List<Note>, order: SortOrder): List<Note> {
        val pinnedFirst = compareByDescending<Note> { it.isPinned }
        val comparator = when (order) {
            SortOrder.NEWEST -> pinnedFirst.thenByDescending { it.updatedAt }
            SortOrder.OLDEST -> pinnedFirst.thenBy { it.updatedAt }
            SortOrder.TITLE -> pinnedFirst.thenBy(String.CASE_INSENSITIVE_ORDER) { it.title }
        }
        return list.sortedWith(comparator)
    }

    fun setQuery(q: String) { query.value = q }
    fun setCategory(c: String?) { selectedCategory.value = c }
    fun setSort(order: SortOrder) { sortOrder.value = order }

    fun setDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean("dark_mode", enabled).apply()
        darkMode.value = enabled
    }

    suspend fun getNote(id: Int): Note? = dao.getById(id)

    fun save(note: Note) = viewModelScope.launch {
        dao.upsert(note.copy(updatedAt = System.currentTimeMillis()))
    }

    // Pinning from the list doesn't change "Updated At"
    fun togglePin(note: Note) = viewModelScope.launch {
        dao.upsert(note.copy(isPinned = !note.isPinned))
    }

    fun delete(note: Note) = viewModelScope.launch { dao.delete(note) }

    // Used by "Undo": puts the note back exactly as it was
    fun restore(note: Note) = viewModelScope.launch { dao.upsert(note) }

    /** Returns an error message, or null if the category was added. */
    fun addCategory(name: String): String? {
        val n = name.trim()
        if (n.isEmpty()) return "Enter a name"
        if (categories.value.any { it.equals(n, ignoreCase = true) }) return "Already exists"
        viewModelScope.launch { categoryDao.insert(Category(n)) }
        return null
    }

    // Notes in a deleted category move to "Other"
    fun deleteCategory(name: String) {
        if (name == "Other") return
        viewModelScope.launch {
            dao.reassignCategory(from = name, to = "Other")
            categoryDao.delete(Category(name))
            if (selectedCategory.value == name) selectedCategory.value = null
        }
    }
}