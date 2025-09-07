package com.example.s66.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReceiptDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ReceiptEntity): Long

    @Query("SELECT * FROM receipt ORDER BY timestamp DESC")
    fun getAll(): Flow<List<ReceiptEntity>>

    @Query("SELECT * FROM receipt WHERE sequenceId LIKE '%' || :q || '%' ORDER BY timestamp DESC")
    fun searchBySequence(q: String): Flow<List<ReceiptEntity>>

    @Query("SELECT * FROM receipt WHERE id = :id")
    suspend fun getById(id: Long): ReceiptEntity?
}
