package com.example.ui.game

import android.app.Activity
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HighScore
import kotlinx.coroutines.delay

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun GameScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.gameState.collectAsState()
    val score by viewModel.score.collectAsState()
    val level by viewModel.level.collectAsState()
    val lives by viewModel.lives.collectAsState()
    val aliensKilled by viewModel.aliensKilled.collectAsState()
    val aliensToKill by viewModel.aliensToKill.collectAsState()
    val usernameInput by viewModel.usernameInput.collectAsState()
    val hasSaveFile by viewModel.hasSaveFile.collectAsState()
    val saveFileDetails by viewModel.saveFileDetails.collectAsState()
    val showSaveMessage by viewModel.showSaveMessage.collectAsState()
    val topScores by viewModel.topScores.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val downloadStatus by viewModel.downloadStatus.collectAsState()
    val isDownloaded by viewModel.isDownloaded.collectAsState()


    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070B1E))
    ) {
        // Starfield Background is always visual across states
        BackgroundStarfield(stars = viewModel.stars)

        AnimatedContent(
            targetState = state,
            transitionSpec = {
                fadeIn() with fadeOut()
            },
            label = "StateTransition"
        ) { currentGameState ->
            when (currentGameState) {
                GameState.STORE_DETAIL -> {
                    StoreDetailScreen(
                        downloadProgress = downloadProgress,
                        downloadStatus = downloadStatus,
                        isDownloaded = isDownloaded,
                        onDownloadStart = { viewModel.startDownloadingGame() }
                    )
                }

                GameState.MENU -> {
                    MainMenuScreen(
                        usernameInput = usernameInput,
                        onUsernameChange = { viewModel.onUsernameInputChanged(it) },
                        hasSaveFile = hasSaveFile,
                        saveFileDetails = saveFileDetails,
                        onStartGame = { viewModel.onStartNewGame() },
                        onContinueGame = { viewModel.onContinueGame() },
                        onExitApp = { (context as? Activity)?.finish() },
                        highScores = topScores
                    )
                }

                GameState.TRANSITION -> {
                    LevelTransitionScreen(level = level)
                }

                GameState.PLAYING, GameState.PAUSED, GameState.GAMEOVER, GameState.WIN -> {
                    // Active Game Loop Canvas Frame
                    ActiveGameScreen(
                        viewModel = viewModel,
                        score = score,
                        level = level,
                        lives = lives,
                        aliensKilled = aliensKilled,
                        aliensToKill = aliensToKill,
                        state = currentGameState,
                        showSaveMessage = showSaveMessage
                    )
                }
            }
        }
    }
}

@Composable
fun BackgroundStarfield(stars: List<Star>) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        stars.forEach { star ->
            drawCircle(
                color = Color.White.copy(alpha = 0.7f),
                radius = star.size,
                center = Offset(
                    x = star.x / 1000f * size.width,
                    y = star.y / 1800f * size.height
                )
            )
        }
    }
}

@Composable
fun MainMenuScreen(
    usernameInput: String,
    onUsernameChange: (String) -> Unit,
    hasSaveFile: Boolean,
    saveFileDetails: String,
    onStartGame: () -> Unit,
    onContinueGame: () -> Unit,
    onExitApp: () -> Unit,
    highScores: List<HighScore>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Title block
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text(
                text = "SPACE",
                fontSize = 44.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Serif,
                color = Color(0xFF00FFFF),
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("menu_title_space")
            )
            Text(
                text = "DEFENDERS",
                fontSize = 40.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Serif,
                color = Color(0xFFFF00FF),
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("menu_title_defenders")
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "NATIVE ARCADE GAME",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.5f),
                letterSpacing = 2.sp
            )
        }

        // Login System Form Box
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Text(
                text = if (usernameInput.trim().isNotEmpty()) {
                    if (hasSaveFile) "👤 Partida de $usernameInput encontrada!" else "👤 Nuevo Jugador: $usernameInput"
                } else {
                    "Ingresa tu nombre para guardar puntuación:"
                },
                color = if (hasSaveFile) Color(0xFF00FF00) else Color(0xFF00FFFF),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = usernameInput,
                onValueChange = onUsernameChange,
                placeholder = { Text("Nombre de usuario", color = Color.White.copy(alpha = 0.4f)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF00FFFF),
                    unfocusedBorderColor = Color(0xFF00FFFF).copy(alpha = 0.4f),
                    focusedContainerColor = Color(0xFF00FFFF).copy(alpha = 0.08f),
                    unfocusedContainerColor = Color.Black.copy(alpha = 0.3f)
                ),
                singleLine = true,
                maxLines = 1,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .testTag("username_input")
            )

            if (hasSaveFile && saveFileDetails.isNotEmpty()) {
                Text(
                    text = "Progreso: $saveFileDetails",
                    color = Color.Yellow,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Scoreboard & Buttons Row / Column spacing
        Column(
            modifier = Modifier.weight(1f, fill = false),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (highScores.isNotEmpty()) {
                Text(
                    text = "🏆 CAPITANES DE ÉLITE",
                    color = Color.Yellow,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(110.dp)
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(highScores) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.username,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "Nv.${item.level}",
                                    color = Color(0xFF00FFFF),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                Text(
                                    text = "${item.score} pts",
                                    color = Color(0xFFFF00FF),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Main Menu Buttons
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Button(
                onClick = onStartGame,
                enabled = usernameInput.trim().isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00FFFF),
                    contentColor = Color.Black,
                    disabledContainerColor = Color(0xFF00FFFF).copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(5.dp),
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(50.dp)
                    .testTag("start_button")
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("NUEVA PARTIDA", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onContinueGame,
                enabled = hasSaveFile && usernameInput.trim().isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF00FF),
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFFFF00FF).copy(alpha = 0.2f)
                ),
                shape = RoundedCornerShape(5.dp),
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(50.dp)
                    .testTag("continue_button")
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("CONTINUAR PARTIDA", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onExitApp,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                shape = RoundedCornerShape(5.dp),
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(44.dp)
                    .testTag("exit_button")
            ) {
                Icon(Icons.Filled.Close, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("SALIR", fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun LevelTransitionScreen(level: Int) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "NIVEL $level",
                fontSize = 54.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.Yellow,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "¡PREPÁRATE CAPITÁN!",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00FFFF),
                letterSpacing = 3.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ActiveGameScreen(
    viewModel: GameViewModel,
    score: Int,
    level: Int,
    lives: Int,
    aliensKilled: Int,
    aliensToKill: Int,
    state: GameState,
    showSaveMessage: Boolean
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val cw = constraints.maxWidth.toFloat()
        val ch = constraints.maxHeight.toFloat()

        val scaleX = cw / 1000f
        val scaleY = ch / 1800f

        val shakeX = if (viewModel.shakeIntensity.value > 0) {
            ((Math.random() - 0.5) * viewModel.shakeIntensity.value * 3f).toFloat()
        } else 0f
        val shakeY = if (viewModel.shakeIntensity.value > 0) {
            ((Math.random() - 0.5) * viewModel.shakeIntensity.value * 3f).toFloat()
        } else 0f

        // MAIN GAME DRAWING CANVAS
        Canvas(modifier = Modifier.fillMaxSize()) {
            translate(left = shakeX, top = shakeY) {
                // Draw Bullets lasers
                viewModel.bullets.forEach { laser ->
                    drawRoundRect(
                        color = Color.Yellow,
                        topLeft = Offset(laser.x * scaleX, laser.y * scaleY),
                        size = Size(laser.width * scaleX, laser.height * scaleY),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f * scaleX)
                    )
                }

                // Draw Aliens
                viewModel.aliens.forEach { alien ->
                    val cx = (alien.x + alien.width / 2f) * scaleX
                    val cy = (alien.y + alien.height / 2f) * scaleY
                    val sizeW = alien.width * scaleX
                    val sizeH = alien.height * scaleY

                    when (alien.type) {
                        "basic" -> {
                            // Pink Sphere Orb with dual eyes
                            drawCircle(
                                color = Color(alien.colorValue),
                                radius = sizeW / 2f,
                                center = Offset(cx, cy)
                            )
                            drawCircle(
                                color = Color.Black,
                                radius = sizeW * 0.12f,
                                center = Offset(cx - sizeW * 0.15f, cy)
                            )
                            drawCircle(
                                color = Color.Black,
                                radius = sizeW * 0.12f,
                                center = Offset(cx + sizeW * 0.15f, cy)
                            )
                        }
                        "fast" -> {
                            // Agile Green Triangle
                            val p1 = Offset(cx, alien.y * scaleY)
                            val p2 = Offset(alien.x * scaleX, (alien.y + alien.height) * scaleY)
                            val p3 = Offset((alien.x + alien.width) * scaleX, (alien.y + alien.height) * scaleY)
                            val triPath = Path().apply {
                                moveTo(p1.x, p1.y)
                                lineTo(p2.x, p2.y)
                                lineTo(p3.x, p3.y)
                                close()
                            }
                            drawPath(path = triPath, color = Color(alien.colorValue))
                        }
                        "tank" -> {
                            // Orange Dreadnought armored box
                            drawRect(
                                color = Color(alien.colorValue),
                                topLeft = Offset(alien.x * scaleX, alien.y * scaleY),
                                size = Size(sizeW, sizeH)
                            )
                            // Double layered protection indicator
                            drawRect(
                                color = Color.Black.copy(alpha = 0.4f),
                                topLeft = Offset((alien.x + 10f) * scaleX, (alien.y + 10f) * scaleY),
                                size = Size(sizeW - 20f * scaleX, sizeH - 20f * scaleY),
                                style = Stroke(width = 3f * scaleX)
                            )
                            // Mini health bar above tanks
                            val hWidth = (alien.hp.toFloat() / alien.maxHp) * sizeW
                            drawRect(
                                color = Color.Red,
                                topLeft = Offset(alien.x * scaleX, (alien.y - 15f) * scaleY),
                                size = Size(hWidth, 6f * scaleY)
                            )
                        }
                        "zigzag" -> {
                            // Yellow Star diamond
                            val path = Path().apply {
                                moveTo(cx, alien.y * scaleY)
                                lineTo((alien.x + alien.width) * scaleX, cy)
                                lineTo(cx, (alien.y + alien.height) * scaleY)
                                lineTo(alien.x * scaleX, cy)
                                close()
                            }
                            drawPath(path = path, color = Color(alien.colorValue))
                        }
                    }
                }

                // Draw Boss if active
                viewModel.bossState.value?.let { b ->
                    val bx = b.x * scaleX
                    val by = b.y * scaleY
                    val bw = b.width * scaleX
                    val bh = b.height * scaleY
                    val bcx = bx + bw / 2f
                    val bcy = by + bh / 2f

                    // Red spaceship structure
                    val path = Path().apply {
                        moveTo(bcx, by)
                        lineTo(bx, bcy)
                        lineTo(bcx, by + bh)
                        lineTo(bx + bw, bcy)
                        close()
                    }
                    drawPath(path = path, color = Color.Red)

                    // Yellow Glowing Shields/eyes
                    drawCircle(
                        color = Color.Yellow,
                        radius = bw * 0.12f,
                        center = Offset(bcx - bw * 0.18f, bcy)
                    )
                    drawCircle(
                        color = Color.Yellow,
                        radius = bw * 0.12f,
                        center = Offset(bcx + bw * 0.18f, bcy)
                    )

                    // Glaring Black pupils
                    drawCircle(
                        color = Color.Black,
                        radius = bw * 0.06f,
                        center = Offset(bcx - bw * 0.18f, bcy)
                    )
                    drawCircle(
                        color = Color.Black,
                        radius = bw * 0.06f,
                        center = Offset(bcx + bw * 0.18f, bcy)
                    )
                }

                // Draw Particles physical sparks
                viewModel.particles.forEach { p ->
                    val alpha = (p.life.toFloat() / p.maxLife).coerceIn(0f, 1f)
                    drawRect(
                        color = Color(p.colorValue).copy(alpha = alpha),
                        topLeft = Offset(p.x * scaleX, p.y * scaleY),
                        size = Size(p.size * scaleX, p.size * scaleY)
                    )
                }

                // Draw Player Space Vessel
                val pXScaled = viewModel.playerX.value * scaleX
                val pYScaled = viewModel.playerY * scaleY
                val pWScaled = viewModel.playerWidth * scaleX
                val pHScaled = viewModel.playerHeight * scaleY

                val playerPath = Path().apply {
                    moveTo(pXScaled + pWScaled / 2f, pYScaled)
                    lineTo(pXScaled, pYScaled + pHScaled)
                    lineTo(pXScaled + pWScaled / 2f, pYScaled + pHScaled - 18f * scaleY)
                    lineTo(pXScaled + pWScaled, pYScaled + pHScaled)
                    close()
                }
                drawPath(path = playerPath, color = Color(0xFF00FFFF))

                // Engine Flame sparklers
                val flameHeight = (15..35).random().toFloat()
                val flamePath = Path().apply {
                    moveTo(pXScaled + pWScaled / 2f - 12f * scaleX, pYScaled + pHScaled)
                    lineTo(pXScaled + pWScaled / 2f, pYScaled + pHScaled + flameHeight * scaleY)
                    lineTo(pXScaled + pWScaled / 2f + 12f * scaleX, pYScaled + pHScaled)
                    close()
                }
                drawPath(path = flamePath, color = Color(0xFFFF6600))

                // Metallic cockpit window
                drawCircle(
                    color = Color.White,
                    radius = pWScaled * 0.12f,
                    center = Offset(pXScaled + pWScaled / 2f, pYScaled + pHScaled * 0.5f)
                )
            }
        }

        // DRAWING FLOATING FLOATS (Score metrics)
        viewModel.floatTexts.forEach { ft ->
            val pXFloat = ft.x * scaleX
            val pYFloat = ft.y * scaleY
            val alphaFloat = (ft.life.toFloat() / 45f).coerceIn(0f, 1f)
            
            Box(
                modifier = Modifier
                    .offset(
                        x = (pXFloat / (cw / maxOf(1, constraints.maxWidth))).dp,
                        y = (pYFloat / (ch / maxOf(1, constraints.maxHeight))).dp
                    )
            ) {
                Text(
                    text = ft.text,
                    color = Color(ft.colorValue).copy(alpha = alphaFloat),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // STATUS PORT - HUD OVERLAY
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // User Scores, levels & health counts
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            .border(1.dp, Color(0xFF00FFFF).copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "⭐ $score",
                            color = Color(0xFF00FFFF),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier.testTag("hud_score")
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            .border(1.dp, Color(0xFF00FFFF).copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "🎯 Nv. $level",
                            color = Color(0xFF00FFFF),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier.testTag("hud_level")
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            .border(1.dp, Color(0xFF00FFFF).copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "❤️ $lives",
                            color = if (lives <= 1) Color.Red else Color(0xFF00FFFF),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier.testTag("hud_lives")
                        )
                    }
                }

                // Pause button
                IconButton(
                    onClick = { viewModel.onPauseGame() },
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color(0xFFFF00FF).copy(alpha = 0.25f), CircleShape)
                        .border(1.5.dp, Color(0xFFFF00FF), CircleShape)
                        .testTag("pause_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Menu,
                        contentDescription = "Pausa",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Level Progress toward Boss
            LinearProgressIndicator(
                progress = { (aliensKilled.toFloat() / aliensToKill).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = Color(0xFF00FFFF),
                trackColor = Color.White.copy(alpha = 0.15f)
            )

            // Boss indicators
            viewModel.bossState.value?.let { boss ->
                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = boss.name.uppercase(),
                        color = Color.Red,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    LinearProgressIndicator(
                        progress = { (boss.hp.toFloat() / boss.maxHp).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .border(1.dp, Color.Red, RoundedCornerShape(5.dp)),
                        color = Color.Red,
                        trackColor = Color.Red.copy(alpha = 0.2f)
                    )
                }
            }
        }

        // SAVE MESSAGE FLOATING NOTIFIER
        if (showSaveMessage) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFF00FF00).copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                        .border(2.dp, Color(0xFF00FF00), RoundedCornerShape(8.dp))
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "💾 PARTIDA GUARDADA",
                        color = Color(0xFF00FF00),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }

        // PAUSE POPUP CONTROLS
        if (state == GameState.PAUSED) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .background(Color(0xFF0A0F30), RoundedCornerShape(12.dp))
                        .border(2.dp, Color(0xFF00FFFF), RoundedCornerShape(12.dp))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "⏸ PAUSA",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00FFFF),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { viewModel.onResumeGame() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFFF), contentColor = Color.Black),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("CONTINUAR JUEGO", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { viewModel.manualSaveGame() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF00), contentColor = Color.Black),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Filled.AccountBox, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("GUARDAR PARTIDA", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { viewModel.onRestartGame() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Yellow, contentColor = Color.Black),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("REINICIAR", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { viewModel.onQuitToMenu() },
                        border = BorderStroke(1.dp, Color.Red),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Filled.Home, contentDescription = null, tint = Color.Red)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("MENÚ PRINCIPAL", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // GAME OVER POPUP CONTROLS
        if (state == GameState.GAMEOVER) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "GAME OVER",
                        fontSize = 44.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Red,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Puntos Obtenidos: $score",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Yellow,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Nivel alcanzado: $level",
                        fontSize = 18.sp,
                        color = Color(0xFF00FFFF),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { viewModel.onRestartGame() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFFF), contentColor = Color.Black),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.fillMaxWidth(0.9f).height(50.dp)
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("REINICIAR", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { viewModel.onQuitToMenu() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.fillMaxWidth(0.9f).height(50.dp)
                    ) {
                        Icon(Icons.Filled.Home, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("MENÚ PRINCIPAL", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // VICTORY WIN POPUP SCREEN
        if (state == GameState.WIN) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "🏆 ¡VICTORIA!",
                        fontSize = 44.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Yellow,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "¡Has salvado al cosmos de la amenaza extraterrestre, capitán!",
                        fontSize = 16.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Puntos Totales: $score",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00FF00),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { viewModel.onRestartGame() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF00), contentColor = Color.Black),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.fillMaxWidth(0.9f).height(50.dp)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("JUGAR NUEVAMENTE", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { viewModel.onQuitToMenu() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.fillMaxWidth(0.9f).height(50.dp)
                    ) {
                        Icon(Icons.Filled.Home, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("MENÚ PRINCIPAL", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // ALWAYS VISIBLE BOTTOM TACTILE HUD LAYOUT
        if (state == GameState.PLAYING) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(180.dp)
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp)
            ) {
                // Moving inputs left & right
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxHeight(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val leftInteractionSource = remember { MutableInteractionSource() }
                    val leftPressed by leftInteractionSource.collectIsPressedAsState()
                    LaunchedEffect(leftPressed) {
                        viewModel.isLeftPressed.value = leftPressed
                    }

                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00FFFF).copy(alpha = if (leftPressed) 0.5f else 0.15f))
                            .border(2.dp, Color(0xFF00FFFF), CircleShape)
                            .clickable(
                                interactionSource = leftInteractionSource,
                                indication = null,
                                onClick = {}
                            )
                            .testTag("left_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowLeft,
                            contentDescription = "Izquierda",
                            tint = Color(0xFF00FFFF),
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    val rightInteractionSource = remember { MutableInteractionSource() }
                    val rightPressed by rightInteractionSource.collectIsPressedAsState()
                    LaunchedEffect(rightPressed) {
                        viewModel.isRightPressed.value = rightPressed
                    }

                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00FFFF).copy(alpha = if (rightPressed) 0.5f else 0.15f))
                            .border(2.dp, Color(0xFF00FFFF), CircleShape)
                            .clickable(
                                interactionSource = rightInteractionSource,
                                indication = null,
                                onClick = {}
                            )
                            .testTag("right_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowRight,
                            contentDescription = "Derecha",
                            tint = Color(0xFF00FFFF),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                // Laser Fire inputs (automated burst rate when clicked & held)
                val fireInteractionSource = remember { MutableInteractionSource() }
                val firePressed by fireInteractionSource.collectIsPressedAsState()
                LaunchedEffect(firePressed) {
                    viewModel.isFiring.value = firePressed
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF00FF).copy(alpha = if (firePressed) 0.5f else 0.15f))
                        .border(3.dp, Color(0xFFFF00FF), CircleShape)
                        .clickable(
                            interactionSource = fireInteractionSource,
                            indication = null,
                            onClick = {}
                        )
                        .testTag("fire_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "DISPARO",
                        color = Color(0xFFFF00FF),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun StoreDetailScreen(
    downloadProgress: Float,
    downloadStatus: String,
    isDownloaded: Boolean,
    onDownloadStart: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1113))
            .padding(16.dp)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // TOP BACK BUTTON & OPTIONS
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {},
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.White.copy(alpha = 0.05f), CircleShape)
            ) {
                Text("←", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = {},
                    modifier = Modifier.size(44.dp).background(Color.White.copy(alpha = 0.05f), CircleShape)
                ) {
                    Text("🔍", color = Color.White, fontSize = 16.sp)
                }
                IconButton(
                    onClick = {},
                    modifier = Modifier.size(44.dp).background(Color.White.copy(alpha = 0.05f), CircleShape)
                ) {
                    Text("⋮", color = Color.White, fontSize = 16.sp)
                }
            }
        }

        // HERO COVER DISPLAY CARD
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                .background(
                    Brush.sweepGradient(
                        colors = listOf(
                            Color(0xFF3D4B63),
                            Color(0xFF1E293B),
                            Color(0xFF4F46E5),
                            Color(0xFF3D4B63)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val gridStep = 45.dp.toPx()
                for (x in 0..size.width.toInt() step gridStep.toInt()) {
                    drawLine(Color.White.copy(alpha = 0.04f), Offset(x.toFloat(), 0f), Offset(x.toFloat(), size.height))
                }
                for (y in 0..size.height.toInt() step gridStep.toInt()) {
                    drawLine(Color.White.copy(alpha = 0.04f), Offset(0f, y.toFloat()), Offset(size.width, y.toFloat()))
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                Text("⚡", fontSize = 68.sp, modifier = Modifier.padding(bottom = 6.dp))
                Text(
                    text = "SPACE\nDEFENDERS",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 2.sp,
                    fontStyle = FontStyle.Italic,
                    textAlign = TextAlign.Center,
                    lineHeight = 36.sp
                )
                Text(
                    text = "ARTISTIC RETRO EDITION",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD0E4FF),
                    letterSpacing = 4.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Genre Tag & Rating overlay badges
            Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Surface(color = Color.White.copy(alpha = 0.15f), shape = RoundedCornerShape(100.dp)) {
                    Text(
                        text = "Action • RPG",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(100.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("★", color = Color(0xFFFFD700), fontSize = 14.sp)
                        Text("4.9", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // GAME NAME & META VERSIONS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Space Defenders",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "v2.0.4",
                color = Color(0xFFA0A3AD),
                fontSize = 14.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                fontWeight = FontWeight.Medium,
                style = androidx.compose.ui.text.TextStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // PARAGRAPH
        Text(
            text = "Sobrevive en una metrópolis futurista espacial. Gráficos retro optimizados de alta fidelidad, controles táctiles continuos de respuesta inmediata y una banda sonora de efectos sintetizados de onda corta analógica.",
            color = Color(0xFFC4C6D0),
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(18.dp))

        // 3 PILL STATS GRID
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(Color(0xFF1B1B1F), RoundedCornerShape(16.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("TAMAÑO", color = Color(0xFF909094), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("8.5MB", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(Color(0xFF1B1B1F), RoundedCornerShape(16.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("DESCARGAS", color = Color(0xFF909094), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("10M+", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(Color(0xFF1B1B1F), RoundedCornerShape(16.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("EDAD", color = Color(0xFF909094), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("12+", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // INTERACTIVE STATE DOWNLOAD CARD BUTTON
        if (downloadProgress == 0f && !isDownloaded) {
            Button(
                onClick = onDownloadStart,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD0E4FF),
                    contentColor = Color(0xFF00315B)
                ),
                shape = RoundedCornerShape(100.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .testTag("download_button")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Descargar ahora", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("↓", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = downloadStatus,
                    color = Color(0xFFD0E4FF),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LinearProgressIndicator(
                    progress = { downloadProgress },
                    color = Color(0xFFD0E4FF),
                    trackColor = Color.White.copy(alpha = 0.1f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${(downloadProgress * 100).toInt()}% completo",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
    }
}
