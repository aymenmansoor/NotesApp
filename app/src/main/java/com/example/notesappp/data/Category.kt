package com.example.notesappp.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "categories")
data class Category(@PrimaryKey val name: String)

@Dao
interface CategoryDao {
    // "Other" always stays last, the rest are alphabetical
    @Query("SELECT * FROM categories ORDER BY (name = 'Other'), name COLLATE NOCASE")
    fun observeAll(): Flow<List<Category>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(category: Category)

    @Delete
    suspend fun delete(category: Category)
}