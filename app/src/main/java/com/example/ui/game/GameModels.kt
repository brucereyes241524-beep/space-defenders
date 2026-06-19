package com.example.ui.game

data class LevelInfo(
    val levelNumber: Int,
    val aliensToKill: Int,
    val alienSpeed: Float,
    val spawnRateMs: Long,
    val types: List<String>,
    val bossHp: Int,
    val bossSpeed: Float,
    val bossName: String
)

data class Star(
    var x: Float,
    var y: Float,
    val size: Float,
    val speed: Float
)

data class Bullet(
    var x: Float,
    var y: Float,
    val width: Float = 10f,
    val height: Float = 40f,
    val speed: Float = 30f,
    val isPlayerBullet: Boolean = true
)

data class Alien(
    var x: Float,
    var y: Float,
    val width: Float,
    val height: Float,
    var hp: Int,
    val maxHp: Int,
    val speed: Float,
    val type: String, // "basic", "fast", "tank", "zigzag"
    val points: Int,
    val colorValue: Long,
    var time: Long = 0,
    val startX: Float
)

data class Boss(
    var x: Float,
    var y: Float,
    val width: Float = 200f,
    val height: Float = 200f,
    var hp: Int,
    val maxHp: Int,
    val speed: Float,
    val name: String,
    var time: Long = 0,
    var direction: Float = 1f,
    var entering: Boolean = true
)

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var life: Int,
    val maxLife: Int,
    val colorValue: Long,
    val size: Float
)

data class FloatText(
    val text: String,
    var x: Float,
    var y: Float,
    val colorValue: Long,
    var life: Int = 45 // Frames remaining
)

enum class GameState {
    STORE_DETAIL,
    MENU,
    PLAYING,
    PAUSED,
    TRANSITION,
    GAMEOVER,
    WIN
}
