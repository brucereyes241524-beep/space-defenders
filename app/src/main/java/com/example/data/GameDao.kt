package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM game_saves WHERE username = :username")
    suspend fun getGameSave(username: String): GameSave?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGameSave(gameSave: GameSave)

    @Query("DELETE FROM game_saves WHERE username = :username")
    suspend fun deleteGameSave(username: String)

    @Query("SELECT * FROM high_scores ORDER BY score DESC, timestamp DESC LIMIT :limit")
    fun getTopScores(limit: Int = 10): Flow<List<HighScore>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHighScore(highScore: HighScore)
}
