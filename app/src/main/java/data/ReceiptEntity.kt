package com.example.s66.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "receipt")
data class ReceiptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sequenceId: String,
    val timestamp: Long,
    val gtTotal: Int,
    val outputText: String,
    val rawInput: String
)
