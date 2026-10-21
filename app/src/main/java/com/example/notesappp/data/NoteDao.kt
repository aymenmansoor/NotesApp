package com.example.notesappp.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query(
        "SELECT * FROM notes " +
                "WHERE (:category IS NULL OR category = :category) " +
                "AND (title LIKE '%' || :q || '%' OR content LIKE '%' || :q || '%')"
    )
    fun observe(q: String, category: String?): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getById(id: Int): Note?

    @Query("UPDATE notes SET category = :to WHERE category = :from")
    suspend fun reassignCategory(from: String, to: String)

    @Upsert
    suspend fun upsert(note: Note)

    @Delete
    suspend fun delete(note: Note)
}