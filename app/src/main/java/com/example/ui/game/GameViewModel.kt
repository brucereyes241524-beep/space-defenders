package com.example.ui.game

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.GameRepository
import com.example.data.GameSave
import com.example.data.HighScore
import com.example.audio.SoundSynthesizer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

class GameViewModel(
    private val repository: GameRepository,
    private val synth: SoundSynthesizer
) : ViewModel() {

    // Target Virtual Screen Coordinates 1000 x 1800
    val virtualWidth = 1000f
    val virtualHeight = 1800f

    // Levels config
    val levelConfigs = listOf(
        LevelInfo(1, 8, 5f, 1300L, listOf("basic"), 20, 2f, "Scout Alien"),
        LevelInfo(2, 10, 6f, 1100L, listOf("basic", "fast"), 30, 3f, "Velocista"),
        LevelInfo(3, 12, 7f, 1000L, listOf("basic", "fast"), 40, 4f, "Destructor"),
        LevelInfo(4, 15, 7f, 900L, listOf("basic", "fast", "tank"), 50, 3f, "Tanque Supremo"),
        LevelInfo(5, 18, 8f, 800L, listOf("basic", "fast", "tank"), 60, 5f, "Comandante"),
        LevelInfo(6, 20, 8f, 750L, listOf("fast", "tank"), 75, 5f, "Élite"),
        LevelInfo(7, 22, 9f, 700L, listOf("fast", "tank", "zigzag"), 90, 4f, "Zigzag Master"),
        LevelInfo(8, 25, 10f, 650L, listOf("fast", "tank", "zigzag"), 110, 6f, "Devastador"),
        LevelInfo(9, 28, 11f, 600L, listOf("tank", "zigzag"), 130, 5f, "Aniquilador"),
        LevelInfo(10, 30, 12f, 500L, listOf("tank", "zigzag", "fast"), 160, 7f, "JEFE FINAL")
    )

    private val _gameState = MutableStateFlow(GameState.STORE_DETAIL)
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    private val _downloadStatus = MutableStateFlow("Descargar ahora")
    val downloadStatus: StateFlow<String> = _downloadStatus.asStateFlow()

    private val _isDownloaded = MutableStateFlow(false)
    val isDownloaded: StateFlow<Boolean> = _isDownloaded.asStateFlow()


    private val _currentUser = MutableStateFlow<String?>(null)
    val currentUser: StateFlow<String?> = _currentUser.asStateFlow()

    private val _score = MutableStateFlow(0)
    val score: StateFlow<Int> = _score.asStateFlow()

    private val _level = MutableStateFlow(1)
    val level: StateFlow<Int> = _level.asStateFlow()

    private val _lives = MutableStateFlow(3)
    val lives: StateFlow<Int> = _lives.asStateFlow()

    private val _aliensKilled = MutableStateFlow(0)
    val aliensKilled: StateFlow<Int> = _aliensKilled.asStateFlow()

    private val _aliensToKill = MutableStateFlow(8)
    val aliensToKill: StateFlow<Int> = _aliensToKill.asStateFlow()

    private val _usernameInput = MutableStateFlow("")
    val usernameInput: StateFlow<String> = _usernameInput.asStateFlow()

    private val _hasSaveFile = MutableStateFlow(false)
    val hasSaveFile: StateFlow<Boolean> = _hasSaveFile.asStateFlow()

    private val _saveFileDetails = MutableStateFlow("")
    val saveFileDetails: StateFlow<String> = _saveFileDetails.asStateFlow()

    private val _showSaveMessage = MutableStateFlow(false)
    val showSaveMessage: StateFlow<Boolean> = _showSaveMessage.asStateFlow()

    val topScores: StateFlow<List<HighScore>> = repository.getTopScores()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Game Entities
    val stars = mutableStateListOf<Star>()
    val bullets = mutableStateListOf<Bullet>()
    val aliens = mutableStateListOf<Alien>()
    val particles = mutableStateListOf<Particle>()
    val floatTexts = mutableStateListOf<FloatText>()

    val bossState = mutableStateOf<Boss?>(null)
    val bossActive = mutableStateOf(false)

    val playerX = mutableStateOf(450f)
    val playerY = 1500f
    val playerWidth = 100f
    val playerHeight = 100f

    val shakeIntensity = mutableStateOf(0)

    // Inputs
    val isLeftPressed = mutableStateOf(false)
    val isRightPressed = mutableStateOf(false)
    val isFiring = mutableStateOf(false)

    private var lastShotTime = 0L
    private var lastSpawnTime = 0L
    private var gameLoopJob: Job? = null

    init {
        initStars()
    }

    private fun initStars() {
        stars.clear()
        for (i in 0 until 60) {
            stars.add(
                Star(
                    x = (0..1000).random().toFloat(),
                    y = (0..1800).random().toFloat(),
                    size = (2..6).random().toFloat(),
                    speed = (2..6).random().toFloat() / 2f
                )
            )
        }
    }

    fun onUsernameInputChanged(name: String) {
        _usernameInput.value = name
        checkSavedGame(name)
    }

    private fun checkSavedGame(username: String) {
        viewModelScope.launch {
            if (username.trim().isEmpty()) {
                _hasSaveFile.value = false
                _saveFileDetails.value = ""
                return@launch
            }
            val save = withContext(Dispatchers.IO) {
                repository.getGameSave(username.trim())
            }
            if (save != null) {
                _hasSaveFile.value = true
                _saveFileDetails.value = "Nivel ${save.level} | ${save.score} Puntos"
            } else {
                _hasSaveFile.value = false
                _saveFileDetails.value = ""
            }
        }
    }

    fun startDownloadingGame() {
        viewModelScope.launch {
            _downloadProgress.value = 0f
            val statusMessages = listOf(
                "Iniciando descarga..." to 0.15f,
                "Buscando satélites de enlace retro..." to 0.35f,
                "Descargando texturas y sprites clásicos..." to 0.6f,
                "Sintetizando archivos de audio arcade..." to 0.82f,
                "Inyectando base de datos Room..." to 0.95f,
                "¡Instalación completa!" to 1.0f
            )

            for ((msg, progress) in statusMessages) {
                _downloadStatus.value = msg
                _downloadProgress.value = progress
                delay(700)
            }
            _isDownloaded.value = true
            _gameState.value = GameState.MENU
            synth.playLevelUp()
        }
    }

    fun onStartNewGame() {
        val name = _usernameInput.value.trim()
        if (name.isEmpty()) return
        _currentUser.value = name
        scoreReset()
        _level.value = 1
        _lives.value = 3
        _aliensKilled.value = 0
        applyLevelConfig(1)
        startNewLevelTransition()
    }

    fun onContinueGame() {
        val name = _currentUser.value ?: _usernameInput.value.trim()
        if (name.isEmpty()) return
        _currentUser.value = name

        viewModelScope.launch {
            val save = withContext(Dispatchers.IO) {
                repository.getGameSave(name)
            }
            if (save != null) {
                _score.value = save.score
                _level.value = save.level
                _lives.value = save.lives
                _aliensKilled.value = save.aliensKilled
                applyLevelConfig(save.level)
                
                // Clear state vectors
                bullets.clear()
                aliens.clear()
                particles.clear()
                floatTexts.clear()
                bossState.value = null
                bossActive.value = false
                
                resetPlayerPosition()
                startPlaying()
            }
        }
    }

    private fun applyLevelConfig(lvlNum: Int) {
        val config = levelConfigs.getOrNull(lvlNum - 1) ?: levelConfigs.last()
        _aliensToKill.value = config.aliensToKill
        // aliensKilled is either loaded or reset
        updateLevelProgress()
    }

    private fun startNewLevelTransition() {
        bullets.clear()
        aliens.clear()
        particles.clear()
        floatTexts.clear()
        bossState.value = null
        bossActive.value = false
        resetPlayerPosition()

        _gameState.value = GameState.TRANSITION
        synth.playLevelUp()

        viewModelScope.launch {
            delay(2200)
            startPlaying()
        }
    }

    private fun startPlaying() {
        _gameState.value = GameState.PLAYING
        lastSpawnTime = System.currentTimeMillis()
        startGameLoop()
    }

    private fun startGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch {
            var lastTime = System.currentTimeMillis()
            while (isActive) {
                val now = System.currentTimeMillis()
                val elapsed = now - lastTime
                if (elapsed >= 16) { // ~60fps
                    if (_gameState.value == GameState.PLAYING) {
                        tick()
                    }
                    lastTime = now
                }
                delay(16)
            }
        }
    }

    private fun tick() {
        // 1. Move Player
        if (isLeftPressed.value) {
            playerX.value = (playerX.value - 15f).coerceAtLeast(0f)
        }
        if (isRightPressed.value) {
            playerX.value = (playerX.value + 15f).coerceAtLeast(virtualWidth - playerWidth)
        }

        // 2. Automate continuous fire
        if (isFiring.value) {
            shoot()
        }

        // 3. Move backgrounds stars
        stars.forEach { star ->
            star.y += star.speed
            if (star.y > virtualHeight) {
                star.y = 0f
                star.x = (0..1000).random().toFloat()
            }
        }

        // 4. Move lasers
        val remainingLasers = mutableListOf<Bullet>()
        bullets.forEach { b ->
            b.y -= b.speed
            if (b.y > -b.height) {
                remainingLasers.add(b)
            }
        }
        bullets.clear()
        bullets.addAll(remainingLasers)

        // 5. Spawn aliens if boss is inactive
        val currentLvl = _level.value
        val config = levelConfigs.getOrNull(currentLvl - 1) ?: levelConfigs.last()

        if (!bossActive.value && _aliensKilled.value < _aliensToKill.value) {
            val nowTime = System.currentTimeMillis()
            if (nowTime - lastSpawnTime >= config.spawnRateMs && aliens.size < 6) {
                spawnAlien(config)
                lastSpawnTime = nowTime
            }
        }

        // 6. Move ordinary aliens
        val remainingAliens = mutableListOf<Alien>()
        aliens.forEach { alien ->
            alien.time++
            alien.y += alien.speed

            if (alien.type == "zigzag") {
                alien.x = alien.startX + kotlin.math.sin(alien.time * 0.05f) * 110f
                alien.x = alien.x.coerceIn(0f, virtualWidth - alien.width)
            }

            // Collapse with player
            if (checkCollision(alien.x, alien.y, alien.width, alien.height, playerX.value, playerY, playerWidth, playerHeight)) {
                handlePlayerHit()
                spawnExplosion(alien.x + alien.width / 2, alien.y + alien.height / 2, alien.colorValue, 15)
                // alien gets killed on player collision
            } else if (alien.y > virtualHeight) {
                // out of bounds, respawn at virtual top to keep difficulty up
                alien.y = -alien.height
                alien.x = (0..900).random().toFloat()
                remainingAliens.add(alien)
            } else {
                remainingAliens.add(alien)
            }
        }
        aliens.clear()
        aliens.addAll(remainingAliens)

        // 7. Move Boss
        bossState.value?.let { b ->
            b.time++
            if (b.entering) {
                b.y += 4f
                if (b.y >= 160f) {
                    b.entering = false
                }
            } else {
                b.x += b.speed * b.direction
                if (b.x <= 0f || b.x >= virtualWidth - b.width) {
                    b.direction *= -1f
                }
                b.y = 160f + kotlin.math.sin(b.time * 0.02f) * 80f
            }

            // Boss collisions with player
            if (checkCollision(b.x, b.y, b.width, b.height, playerX.value, playerY, playerWidth, playerHeight)) {
                handlePlayerHit()
                spawnExplosion(playerX.value + playerWidth / 2, playerY + playerHeight / 2, 0xFFFF0000L, 20)
            }
        }

        // 8. Collate lasers and target collision checks
        val lasersToRemove = mutableSetOf<Bullet>()
        val aliensToRemove = mutableSetOf<Alien>()

        bullets.forEach { b ->
            // Check Boss first
            bossState.value?.let { bstate ->
                if (checkCollision(b.x, b.y, b.width, b.height, bstate.x, bstate.y, bstate.width, bstate.height)) {
                    lasersToRemove.add(b)
                    bstate.hp--
                    spawnExplosion(b.x, b.y, 0xFFFFFFFFL, 4)
                    if (bstate.hp <= 0) {
                        handleBossDefeated()
                    } else {
                        synth.playHit()
                    }
                }
            }

            if (b in lasersToRemove) return@forEach

            // Check Ordinary aliens
            aliens.forEach { alien ->
                if (alien !in aliensToRemove && checkCollision(b.x, b.y, b.width, b.height, alien.x, alien.y, alien.width, alien.height)) {
                    lasersToRemove.add(b)
                    alien.hp--
                    spawnExplosion(b.x, b.y, 0xFFFFFFFFL, 4)
                    if (alien.hp <= 0) {
                        aliensToRemove.add(alien)
                        handleAlienKilled(alien)
                    } else {
                        synth.playHit()
                    }
                }
            }
        }

        bullets.removeAll(lasersToRemove)
        aliens.removeAll(aliensToRemove)

        // 9. Move particles
        val remainingParticles = mutableListOf<Particle>()
        particles.forEach { p ->
            p.x += p.vx
            p.y += p.vy
            p.vy += 0.3f // gravity influence
            p.life--
            if (p.life > 0) {
                remainingParticles.add(p)
            }
        }
        particles.clear()
        particles.addAll(remainingParticles)

        // 10. Update text indicators
        val remainingTexts = mutableListOf<FloatText>()
        floatTexts.forEach { f ->
            f.y -= 2.5f
            f.life--
            if (f.life > 0) {
                remainingTexts.add(f)
            }
        }
        floatTexts.clear()
        floatTexts.addAll(remainingTexts)

        // 11. Shake decay
        if (shakeIntensity.value > 0) {
            shakeIntensity.value--
        }
    }

    private fun spawnAlien(config: LevelInfo) {
        val type = config.types.random()
        val startX = (50..850).random().toFloat()
        
        var w = 80f
        var h = 80f
        var hp = 1
        var p = 10
        var colVal = 0xFFFF00FFL // purple
        var speedVal = config.alienSpeed

        when (type) {
            "fast" -> {
                w = 60f
                h = 60f
                hp = 1
                p = 15
                colVal = 0xFF00FF00L // green
                speedVal *= 1.5f
            }
            "tank" -> {
                w = 110f
                h = 110f
                hp = 3
                p = 30
                colVal = 0xFFFF6600L // orange
                speedVal *= 0.7f
            }
            "zigzag" -> {
                w = 80f
                h = 80f
                hp = 2
                p = 20
                colVal = 0xFFFFFF00L // yellow
            }
        }

        aliens.add(
            Alien(
                x = startX,
                y = -100f,
                width = w,
                height = h,
                hp = hp,
                maxHp = hp,
                speed = speedVal,
                type = type,
                points = p,
                colorValue = colVal,
                startX = startX
            )
        )
    }

    private fun spawnExplosion(x: Float, y: Float, colorVal: Long, size: Int = 12) {
        for (i in 0 until size) {
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = ((Math.random() - 0.5) * 16).toFloat(),
                    vy = ((Math.random() - 0.5) * 16).toFloat(),
                    life = (20..40).random(),
                    maxLife = 40,
                    colorValue = colorVal,
                    size = (4..12).random().toFloat()
                )
            )
        }
    }

    private fun handlePlayerHit() {
        shakeIntensity.value = 15
        synth.playHit()
        _lives.value--
        if (_lives.value <= 0) {
            handleGameOver()
        }
    }

    private fun handleAlienKilled(alien: Alien) {
        synth.playExplosion()
        _score.value += alien.points
        _aliensKilled.value++
        updateLevelProgress()

        floatTexts.add(
            FloatText(
                text = "+${alien.points}",
                x = alien.x,
                y = alien.y,
                colorValue = alien.colorValue
            )
        )

        // Spawn Boss when threshold reached
        if (_aliensKilled.value >= _aliensToKill.value && !bossActive.value) {
            spawnBoss()
        }
    }

    private fun spawnBoss() {
        val currentLvl = _level.value
        val config = levelConfigs.getOrNull(currentLvl - 1) ?: levelConfigs.last()

        bossState.value = Boss(
            x = 400f,
            y = -220f,
            hp = config.bossHp,
            maxHp = config.bossHp,
            speed = config.bossSpeed,
            name = config.bossName
        )
        bossActive.value = true
        synth.playBossSpawn()
    }

    private fun handleBossDefeated() {
        val b = bossState.value ?: return
        spawnExplosion(b.x + b.width / 2, b.y + b.height / 2, 0xFFFF0000L, 35)
        synth.playExplosion()

        _score.value += 500
        floatTexts.add(
            FloatText(
                text = "+500 JEFE!",
                x = b.x + 50f,
                y = b.y,
                colorValue = 0xFFFF0000L
            )
        )

        bossState.value = null
        bossActive.value = false

        if (_level.value >= levelConfigs.size) {
            handleVictory()
        } else {
            // Auto-save on next level transition
            val name = _currentUser.value ?: _usernameInput.value.trim()
            if (name.isNotEmpty()) {
                viewModelScope.launch(Dispatchers.IO) {
                    repository.saveGame(
                        GameSave(
                            username = name,
                            level = _level.value + 1,
                            score = _score.value,
                            lives = _lives.value,
                            aliensKilled = 0
                        )
                    )
                }
            }
            
            _level.value++
            applyLevelConfig(_level.value)
            startNewLevelTransition()
        }
    }

    fun manualSaveGame() {
        val name = _currentUser.value ?: _usernameInput.value.trim()
        if (name.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            repository.saveGame(
                GameSave(
                    username = name,
                    level = _level.value,
                    score = _score.value,
                    lives = _lives.value,
                    aliensKilled = _aliensKilled.value
                )
            )
            // Trigger temporary visual safe text
            _showSaveMessage.value = true
            delay(2000)
            _showSaveMessage.value = false
        }
    }

    private fun handleGameOver() {
        _gameState.value = GameState.GAMEOVER
        synth.playGameOver()
        gameLoopJob?.cancel()

        // Submit to scoreboard and clear saved progress
        val name = _currentUser.value ?: _usernameInput.value.trim()
        if (name.isNotEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.saveHighScore(
                    HighScore(
                        username = name,
                        score = _score.value,
                        level = _level.value
                    )
                )
                repository.deleteGameSave(name)
            }
        }
    }

    private fun handleVictory() {
        _gameState.value = GameState.WIN
        synth.playLevelUp()
        gameLoopJob?.cancel()

        val name = _currentUser.value ?: _usernameInput.value.trim()
        if (name.isNotEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.saveHighScore(
                    HighScore(
                        username = name,
                        score = _score.value,
                        level = _level.value
                    )
                )
                repository.deleteGameSave(name)
            }
        }
    }

    fun onPauseGame() {
        _gameState.value = GameState.PAUSED
    }

    fun onResumeGame() {
        _gameState.value = GameState.PLAYING
        startGameLoop()
    }

    fun onRestartGame() {
        _score.value = 0
        _lives.value = 3
        _level.value = 1
        _aliensKilled.value = 0
        applyLevelConfig(1)
        startNewLevelTransition()
    }

    fun onQuitToMenu() {
        gameLoopJob?.cancel()
        _gameState.value = GameState.MENU
        checkSavedGame(_usernameInput.value)
    }

    fun shoot() {
        if (_gameState.value != GameState.PLAYING) return
        val now = System.currentTimeMillis()
        if (now - lastShotTime < 240) return
        lastShotTime = now

        bullets.add(
            Bullet(
                x = playerX.value + playerWidth / 2f - 5f,
                y = playerY - 30f
            )
        )
        synth.playShoot()
    }

    private fun resetPlayerPosition() {
        playerX.value = (virtualWidth - playerWidth) / 2f
    }

    private fun scoreReset() {
        _score.value = 0
    }

    private fun updateLevelProgress() {
        // Safe check
    }

    private fun checkCollision(
        x1: Float, y1: Float, w1: Float, h1: Float,
        x2: Float, y2: Float, w2: Float, h2: Float
    ): Boolean {
        return x1 < x2 + w2 &&
               x1 + w1 > x2 &&
               y1 < y2 + h2 &&
               y1 + h1 > y2
    }

    override fun onCleared() {
        super.onCleared()
        gameLoopJob?.cancel()
    }
}

class GameViewModelFactory(
    private val repository: GameRepository,
    private val synth: SoundSynthesizer
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GameViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GameViewModel(repository, synth) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
