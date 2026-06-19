package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_saves")
data class GameSave(
    @PrimaryKey val username: String,
    val level: Int,
    val score: Int,
    val lives: Int,
    val aliensKilled: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "high_scores")
data class HighScore(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String,
    val score: Int,
    val level: Int,
    val timestamp: Long = System.currentTimeMillis()
)
