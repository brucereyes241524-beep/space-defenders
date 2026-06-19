package com.example.data

import kotlinx.coroutines.flow.Flow

class GameRepository(private val gameDao: GameDao) {
    suspend fun getGameSave(username: String): GameSave? {
        return gameDao.getGameSave(username)
    }

    suspend fun saveGame(gameSave: GameSave) {
        gameDao.insertGameSave(gameSave)
    }

    suspend fun deleteGameSave(username: String) {
        gameDao.deleteGameSave(username)
    }

    fun getTopScores(limit: Int = 10): Flow<List<HighScore>> {
        return gameDao.getTopScores(limit)
    }

    suspend fun saveHighScore(highScore: HighScore) {
        gameDao.insertHighScore(highScore)
    }
}
