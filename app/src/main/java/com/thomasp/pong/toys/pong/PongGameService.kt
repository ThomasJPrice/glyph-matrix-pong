package com.thomasjprice.pong.toys.pong

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.VibrationEffect
import android.os.Vibrator
import com.thomasjprice.pong.PongSettings
import com.thomasjprice.pong.R
import com.thomasjprice.pong.toys.GlyphMatrixService
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphMatrixFrame
import com.nothing.ketchum.GlyphMatrixObject
import com.thomasjprice.pong.toys.pong.game.GameState
import com.thomasjprice.pong.toys.pong.game.LevelManager
import kotlinx.coroutines.*
import kotlin.math.roundToInt

class PongGameService : GlyphMatrixService("Pong-Game"), SensorEventListener {
    private lateinit var levelManager: LevelManager
    private var gameState: GameState = GameState.START_SCREEN

    private var sensorManager: SensorManager? = null
    private val gameScope = CoroutineScope(Dispatchers.Default)
    private var gameLoop: Job? = null
    private var isGameRunning = false
    private val ball = Ball()
    private var vibrator: Vibrator? = null

    // Sensor and paddle control variables
    private var rotationSensor: Sensor? = null
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    // Bot AI variables
    private var botPointerIndex: Int = BOT_PADDLE_START_INDEX
    private var lastReactionTime: Long = 0L
    private var botTargetIndex: Int = BOT_PADDLE_START_INDEX
    private var botShouldMakeMistake: Boolean = false
    private var botMistakeError: Int = 0
    private var lastBallDirection: Float = 0f

    // Sound variables
    private var soundPool: SoundPool? = null
    private var isPlayingSound = false
    private var userHitSoundId = 0
    private var botHitSoundId = 0
    private var winSoundId = 0
    private var loseSoundId = 0

    companion object {
        private const val MATRIX_SIZE = 25
        private const val DEVICE_PHONE3 = "23112"
        private const val FRAME_RATE = 60L
        private const val FRAME_TIME = 1000L / FRAME_RATE
        private const val INITIAL_BALL_SPEED = 0.2f  // Increased from 0.1f
        private const val BOT_PADDLE_START_INDEX = 13  // Center position for bot paddle
        private const val BOT_PADDLE_OPACITY = 1024  // 50% opacity
        private const val BALL_SIZE = 2  // 2x2 pixel ball
        private const val BOT_ERROR_CHANCE = 0.15f  // Reduced from 0.3 for better initial performance
        private const val BOT_REACTION_DELAY_MS = 200L  // Reduced from 200ms for quicker reactions
        private const val SCROLL_RESET_POSITION = 80f  // Position to reset scroll
        private const val SCROLL_START_X = 25  // Where text starts scrolling from
        private const val SCROLL_SPEED = 0.2f
        private const val MIN_X_VELOCITY = 0.5f

        // Wall constants
        private val TOP_WALL_CELLS = (9..15).map { x -> Pair(x, 1) }.toSet()
        private val BOTTOM_WALL_CELLS = (9..15).map { x -> Pair(x, 23) }.toSet()
        private val ALL_WALL_CELLS = TOP_WALL_CELLS + BOTTOM_WALL_CELLS

        // Font system constants
        private const val CHAR_WIDTH = 3
        private const val CHAR_HEIGHT = 5
        private const val CHAR_SPACING = 1

        // Font definition - each character is 3x5 pixels
        private val FONT_CHARS = mapOf(
            'A' to arrayOf(
                intArrayOf(1,1,1),
                intArrayOf(1,0,1),
                intArrayOf(1,1,1),
                intArrayOf(1,0,1),
                intArrayOf(1,0,1)
            ),
            'B' to arrayOf(
                intArrayOf(1,1,0),
                intArrayOf(1,0,1),
                intArrayOf(1,1,0),
                intArrayOf(1,0,1),
                intArrayOf(1,1,0)
            ),
            'C' to arrayOf(
                intArrayOf(1,1,1),
                intArrayOf(1,0,0),
                intArrayOf(1,0,0),
                intArrayOf(1,0,0),
                intArrayOf(1,1,1)
            ),
            'D' to arrayOf(
                intArrayOf(1,1,0),
                intArrayOf(1,0,1),
                intArrayOf(1,0,1),
                intArrayOf(1,0,1),
                intArrayOf(1,1,0)
            ),
            'E' to arrayOf(
                intArrayOf(1,1,1),
                intArrayOf(1,0,0),
                intArrayOf(1,1,1),
                intArrayOf(1,0,0),
                intArrayOf(1,1,1)
            ),
            'F' to arrayOf(
                intArrayOf(1,1,1),
                intArrayOf(1,0,0),
                intArrayOf(1,1,1),
                intArrayOf(1,0,0),
                intArrayOf(1,0,0)
            ),
            'G' to arrayOf(
                intArrayOf(1,1,1),
                intArrayOf(1,0,0),
                intArrayOf(1,0,1),
                intArrayOf(1,0,1),
                intArrayOf(1,1,1)
            ),
            'H' to arrayOf(
                intArrayOf(1,0,1),
                intArrayOf(1,0,1),
                intArrayOf(1,1,1),
                intArrayOf(1,0,1),
                intArrayOf(1,0,1)
            ),
            'I' to arrayOf(
                intArrayOf(1,1,1),
                intArrayOf(0,1,0),
                intArrayOf(0,1,0),
                intArrayOf(0,1,0),
                intArrayOf(1,1,1)
            ),
            'L' to arrayOf(
                intArrayOf(1,0,0),
                intArrayOf(1,0,0),
                intArrayOf(1,0,0),
                intArrayOf(1,0,0),
                intArrayOf(1,1,1)
            ),
            'N' to arrayOf(
                intArrayOf(1,1,1),
                intArrayOf(1,0,1),
                intArrayOf(1,0,1),
                intArrayOf(1,0,1),
                intArrayOf(1,0,1)
            ),
            'O' to arrayOf(
                intArrayOf(1,1,1),
                intArrayOf(1,0,1),
                intArrayOf(1,0,1),
                intArrayOf(1,0,1),
                intArrayOf(1,1,1)
            ),
            'P' to arrayOf(
                intArrayOf(1,1,1),
                intArrayOf(1,0,1),
                intArrayOf(1,1,1),
                intArrayOf(1,0,0),
                intArrayOf(1,0,0)
            ),
            'U' to arrayOf(
                intArrayOf(1,0,1),
                intArrayOf(1,0,1),
                intArrayOf(1,0,1),
                intArrayOf(1,0,1),
                intArrayOf(1,1,1)
            ),
            'V' to arrayOf(
                intArrayOf(1,0,1),
                intArrayOf(1,0,1),
                intArrayOf(1,0,1),
                intArrayOf(1,0,1),
                intArrayOf(0,1,0)
            ),
            'Y' to arrayOf(
                intArrayOf(1,0,1),
                intArrayOf(1,0,1),
                intArrayOf(0,1,0),
                intArrayOf(0,1,0),
                intArrayOf(0,1,0)
            ),
            'S' to arrayOf(
                intArrayOf(1,1,1),
                intArrayOf(1,0,0),
                intArrayOf(1,1,1),
                intArrayOf(0,0,1),
                intArrayOf(1,1,1)
            ),
            ' ' to arrayOf(
                intArrayOf(0,0,0),
                intArrayOf(0,0,0),
                intArrayOf(0,0,0),
                intArrayOf(0,0,0),
                intArrayOf(0,0,0)
            ),
            '-' to arrayOf(
                intArrayOf(0,0,0),
                intArrayOf(0,0,0),
                intArrayOf(1,1,1),
                intArrayOf(0,0,0),
                intArrayOf(0,0,0)
            ),
            '0' to arrayOf(
                intArrayOf(1,1,1),
                intArrayOf(1,0,1),
                intArrayOf(1,0,1),
                intArrayOf(1,0,1),
                intArrayOf(1,1,1)
            ),
            '1' to arrayOf(
                intArrayOf(0,1,0),
                intArrayOf(1,1,0),
                intArrayOf(0,1,0),
                intArrayOf(0,1,0),
                intArrayOf(1,1,1)
            ),
            '2' to arrayOf(
                intArrayOf(1,1,1),
                intArrayOf(0,0,1),
                intArrayOf(1,1,1),
                intArrayOf(1,0,0),
                intArrayOf(1,1,1)
            ),
            '3' to arrayOf(
                intArrayOf(1,1,1),
                intArrayOf(0,0,1),
                intArrayOf(0,1,1),
                intArrayOf(0,0,1),
                intArrayOf(1,1,1)
            ),
            '4' to arrayOf(
                intArrayOf(1,0,1),
                intArrayOf(1,0,1),
                intArrayOf(1,1,1),
                intArrayOf(0,0,1),
                intArrayOf(0,0,1)
            ),
            '5' to arrayOf(
                intArrayOf(1,1,1),
                intArrayOf(1,0,0),
                intArrayOf(1,1,1),
                intArrayOf(0,0,1),
                intArrayOf(1,1,1)
            ),
            '6' to arrayOf(
                intArrayOf(1,1,1),
                intArrayOf(1,0,0),
                intArrayOf(1,1,1),
                intArrayOf(1,0,1),
                intArrayOf(1,1,1)
            ),
            '7' to arrayOf(
                intArrayOf(1,1,1),
                intArrayOf(0,0,1),
                intArrayOf(0,1,0),
                intArrayOf(0,1,0),
                intArrayOf(0,1,0)
            ),
            '8' to arrayOf(
                intArrayOf(1,1,1),
                intArrayOf(1,0,1),
                intArrayOf(1,1,1),
                intArrayOf(1,0,1),
                intArrayOf(1,1,1)
            ),
            '9' to arrayOf(
                intArrayOf(1,1,1),
                intArrayOf(1,0,1),
                intArrayOf(1,1,1),
                intArrayOf(0,0,1),
                intArrayOf(1,1,1)
            )
        )
    }

    // Game settings
    private var isPlaying = false
    private var pointerIndex: Int = 13

    // List of LED coordinates for the left edge (from bottom to top)
    private val paddlePositions = mutableListOf(
        Pair(8, 23),  // Bottom
        Pair(7, 23),
        Pair(6, 22),
        Pair(5, 22),
        Pair(4, 21),
        Pair(3, 20),
        Pair(2, 19),
        Pair(2, 18),
        Pair(1, 17),
        Pair(1, 16),
        Pair(0, 15),
        Pair(0, 14),
        Pair(0, 13),
        Pair(0, 12),  // Starting position (center)
        Pair(0, 11),
        Pair(0, 10),
        Pair(0, 9),
        Pair(1, 8),
        Pair(1, 7),
            Pair(2, 6),
        Pair(2, 5),
        Pair(3, 4),
        Pair(4, 3),
        Pair(5, 2),
        Pair(6, 2),
        Pair(7, 1),
        Pair(8, 1)    // Top
    )

    // Physics state for paddle movement
    private var paddlePosition: Float = 13f         // Position along the track (0 to paddlePositions.size-1)

    private var scrollJob: Job? = null
    private var startScreenScrollPosition = 0f

    // Add new variables for smooth movement
    private var botCurrentPosition: Float = BOT_PADDLE_START_INDEX.toFloat()
    private val BOT_MOVEMENT_SPEED = 0.15f  // Adjust this value to change smoothness

    override fun performOnServiceConnected(context: Context, glyphMatrixManager: GlyphMatrixManager) {
        glyphMatrixManager.register(DEVICE_PHONE3)
        vibrator = context.getSystemService(Vibrator::class.java)
        levelManager = LevelManager(context)
        setupSensors(context)
        setupSoundPool(context)
        startGameLoop()
        startScrollingText()
        updateDisplay()
    }

    private fun setupSoundPool(context: Context) {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(1)  // Only allow one sound at a time
            .setAudioAttributes(audioAttributes)
            .build()

        // Load sound effects
        userHitSoundId = soundPool?.load(context, R.raw.user_hit, 1) ?: 0
        botHitSoundId = soundPool?.load(context, R.raw.bot_hit, 1) ?: 0
        winSoundId = soundPool?.load(context, R.raw.win, 1) ?: 0
        loseSoundId = soundPool?.load(context, R.raw.lose, 1) ?: 0
    }

    private fun playSound(soundId: Int) {
        if (!isPlayingSound && soundId != 0 && PongSettings.soundEnabled) {
            isPlayingSound = true
            soundPool?.play(soundId, 1f, 1f, 1, 0, 1f)
            gameScope.launch {
                delay(100) // Prevent new sounds for 100ms
                isPlayingSound = false
            }
        }
    }

    override fun performOnServiceDisconnected(context: Context) {
        stopGame()
        stopScrollingText()
        sensorManager?.unregisterListener(this)
        gameScope.cancel()

        // Release sound pool
        soundPool?.release()
        soundPool = null
    }

    override fun onTouchPointLongPress() {
        when (gameState) {
            GameState.START_SCREEN -> {
                stopScrollingText()
                gameState = GameState.PLAYING
                isPlaying = true
                ball.start()
                vibrate(50)
                updateDisplay()
            }
            GameState.LEVEL_UP, GameState.GAME_OVER -> {
                gameState = GameState.START_SCREEN
                startScrollingText()
                updateDisplay()
            }
            else -> { }
        }
    }

    private fun startGameLoop() {
        if (!isGameRunning) {
            isGameRunning = true
            gameLoop = gameScope.launch {
                while (isActive && isGameRunning) {
                    if (isPlaying) {
                        updateGame()
                    }
                    updateDisplay()
                    delay(FRAME_TIME)
                }
            }
        }
    }

    private fun stopGame() {
        isGameRunning = false
        gameLoop?.cancel()
    }

    private fun updateGame() {
        if (gameState != GameState.PLAYING) return

        // Update ball speed based on level
        val speedMultiplier = levelManager.getBallSpeedMultiplier()
        ball.update(speedMultiplier)

        // Update bot error chance based on level
        val botErrorChance = levelManager.getBotErrorPercentage()

        if (ball.velocityX > 0 && lastBallDirection <= 0) {
            botShouldMakeMistake = Math.random() < botErrorChance
            botMistakeError = if (botShouldMakeMistake) {
                (Math.random() * 8 - 4).toInt()
            } else 0
        }
        lastBallDirection = ball.velocityX
        updateBotPaddle()

        // Handle wall collisions and scoring
        val ballLeft = ball.x
        val ballRight = ball.x + BALL_SIZE
        val ballTop = ball.y
        val ballBottom = ball.y + BALL_SIZE

        // Check for collisions with specific wall cells
        var hitWall = false
        for (dx in 0 until BALL_SIZE) {
            for (dy in 0 until BALL_SIZE) {
                val checkX = (ballLeft + dx).toInt()
                val checkY = (ballTop + dy).toInt()
                if (ALL_WALL_CELLS.contains(Pair(checkX, checkY))) {
                    hitWall = true
                    break
                }
            }
            if (hitWall) break
        }

        if (hitWall) {
            ball.reverseY()
            // Move ball away from wall to prevent sticking
            if (ballTop <= 2) { // Near top wall
                ball.y = 2f
            } else { // Near bottom wall
                ball.y = 22f - BALL_SIZE
            }
            return
        }

        // Left wall (x=0) collision - Player loses point
        if (ballLeft <= 0) {
            resetAfterPoint(false)
            return
        }

        // Right wall (x=24) collision - Player scores point
        if (ballRight >= MATRIX_SIZE) {
            resetAfterPoint(true)
            return
        }

        // Top and bottom wall collisions (y=0 and y=24) - only score if not in wall area
        if (ballTop <= 0 || ballBottom >= MATRIX_SIZE) {
            // Check if we're in the wall zone (x=9 to x=15)
            if (ballLeft >= 9 && ballRight <= 15) {
                ball.reverseY()
                // Ensure the ball doesn't get stuck in the wall
                if (ballTop <= 0) {
                    ball.y = 0f
                } else {
                    ball.y = (MATRIX_SIZE - BALL_SIZE).toFloat()
                }
            } else {
                // Outside wall zone, check which player scored
                if (ballLeft < MATRIX_SIZE / 2) {
                    resetAfterPoint(false) // Bot scores
                } else {
                    resetAfterPoint(true)  // Player scores
                }
                return
            }
        }

        // Check paddle collisions
        // For player paddle (left side)
        if (ball.velocityX < 0) { // Only check when ball is moving towards player
            val paddleRange = (pointerIndex - 2)..(pointerIndex + 2)
            var hitPaddle = false

            for (i in paddleRange) {
                if (i !in paddlePositions.indices) continue
                val (px, py) = paddlePositions[i]

                // Check if any part of the ball overlaps with this paddle pixel
                if (ballLeft <= (px + 1) && ballRight >= px &&
                    ballTop <= (py + 1) && ballBottom >= py) {
                    hitPaddle = true
                    break
                }
            }

            if (hitPaddle) {
                // Calculate deflection based on where the ball hit relative to paddle center
                val paddleCenterY = paddlePositions[pointerIndex].second
                val hitOffset = (ballTop + BALL_SIZE / 2) - paddleCenterY

                // Reverse X direction and ensure it maintains speed
                ball.reverseX()

                // Calculate Y velocity based on hit position, but ensure it's directed towards opponent
                val hitFactor = (hitOffset * 0.15f).coerceIn(-0.8f, 0.8f)
                ball.velocityY = hitFactor * ball.getCurrentSpeed()

                // Move ball just outside paddle to prevent multiple hits
                ball.x = paddlePositions[pointerIndex].first + 1f
                vibrate(50)
                playSound(userHitSoundId)
                return
            }
        }

        // For bot paddle (right side)
        if (ball.velocityX > 0) { // Only check when ball is moving towards bot
            val paddleRange = (botPointerIndex - 2)..(botPointerIndex + 2)
            var hitPaddle = false

            for (i in paddleRange) {
                if (i !in paddlePositions.indices) continue
                val (x, y) = paddlePositions[i]
                val botX = MATRIX_SIZE - 1 - x

                // Check if any part of the ball overlaps with this paddle pixel
                if (ballRight >= botX && ballLeft <= (botX + 1) &&
                    ballTop <= (y + 1) && ballBottom >= y) {
                    hitPaddle = true
                    break
                }
            }

            if (hitPaddle) {
                // Calculate deflection based on where the ball hit relative to paddle center
                val paddleCenterY = paddlePositions[botPointerIndex].second
                val hitOffset = (ballTop + BALL_SIZE / 2) - paddleCenterY

                // Reverse X direction and ensure it maintains speed
                ball.reverseX()

                // Calculate Y velocity based on hit position, but ensure it's directed towards opponent
                val hitFactor = (hitOffset * 0.15f).coerceIn(-0.8f, 0.8f)
                ball.velocityY = hitFactor * ball.getCurrentSpeed()

                // Move ball just outside paddle to prevent multiple hits
                ball.x = (MATRIX_SIZE - 1 - paddlePositions[botPointerIndex].first - BALL_SIZE - 1f)
                vibrate(50)
                playSound(botHitSoundId)
                return
            }
        }
    }

    private fun isNearPoint(x1: Float, y1: Float, x2: Float, y2: Float, tolerance: Float = 1.0f): Boolean {
        val dx = x1 - x2
        val dy = y1 - y2
        return Math.sqrt((dx * dx + dy * dy).toDouble()) <= tolerance.toDouble()
    }

    private fun vibrate(duration: Long) {
        if (!PongSettings.hapticEnabled) return

        vibrator?.let { v ->
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(duration)
            }
        }
    }

    private fun updateBotPaddle() {
        val now = System.currentTimeMillis()
        if (now - lastReactionTime >= BOT_REACTION_DELAY_MS) {
            lastReactionTime = now
            // Mirror the ball's Y position for the bot's side
            val ballY = (MATRIX_SIZE - 1 - ball.y).toInt()
            val error = botMistakeError
            botTargetIndex = (ballY + error).coerceIn(2, paddlePositions.size - 3)
        }

        // Smoothly interpolate the bot's position
        val diff = botTargetIndex - botCurrentPosition
        if (diff != 0f) {
            botCurrentPosition += diff * BOT_MOVEMENT_SPEED
            botPointerIndex = botCurrentPosition.roundToInt().coerceIn(2, paddlePositions.size - 3)
        }
    }

    private fun resetAfterPoint(playerScored: Boolean) {
        if (playerScored) {
            // Player wins, level up
            gameState = GameState.LEVEL_UP
            isPlaying = false
            ball.reset()
            // Level up only after state changes to prevent race conditions
            levelManager.levelUp()
            vibrate(100) // Victory vibration
            playSound(winSoundId)

            // Schedule transition to start screen after 2 seconds
            gameScope.launch {
                delay(2000)
                gameState = GameState.START_SCREEN
                startScreenScrollPosition = 0f  // Reset scroll position
                startScrollingText()  // Restart scrolling
                updateDisplay()
            }
        } else {
            // Player loses, reset to level 0
            gameState = GameState.GAME_OVER
            isPlaying = false
            ball.reset()
            levelManager.resetToStart()
            vibrate(300) // Longer vibration for loss
            playSound(loseSoundId)

            // Schedule transition to start screen after 2 seconds
            gameScope.launch {
                delay(2000)
                gameState = GameState.START_SCREEN
                startScreenScrollPosition = 0f  // Reset scroll position
                startScrollingText()  // Restart scrolling
                updateDisplay()
            }
        }
        updateDisplay()
    }

    private fun updateDisplay() {
        glyphMatrixManager?.let { manager ->
            when (gameState) {
                GameState.START_SCREEN -> renderStartScreen()
                GameState.PLAYING -> {
                    val matrix = Array(MATRIX_SIZE) { IntArray(MATRIX_SIZE) }
                    renderGameScreen(matrix)
                    manager.setMatrixFrame(matrix.flatMap { it.asIterable() }.toIntArray())
                }
                GameState.LEVEL_UP -> renderLevelUpScreen()
                GameState.GAME_OVER -> renderGameOverScreen()
            }
        }
    }

    private fun renderLevelUpScreen() {
        glyphMatrixManager?.let { manager ->
            val matrix = Array(MATRIX_SIZE) { IntArray(MATRIX_SIZE) }

            // Center "LEVEL" text
            drawText(matrix, "LEVEL", 3, 7)
            // Center "UP" text
            drawText(matrix, "UP", 9, 13)

            manager.setMatrixFrame(matrix.flatMap { it.asIterable() }.toIntArray())
        }
    }

    private fun renderGameOverScreen() {
        glyphMatrixManager?.let { manager ->
            val matrix = Array(MATRIX_SIZE) { IntArray(MATRIX_SIZE) }

            // Center "YOU" text
            drawText(matrix, "YOU", 7, 7)
            // Center "LOSE" text
            drawText(matrix, "LOSE", 5, 13)

            manager.setMatrixFrame(matrix.flatMap { it.asIterable() }.toIntArray())
        }
    }

    private fun drawChar(matrix: Array<IntArray>, char: Char, x: Int, y: Int, brightness: Int = 2047) {
        val charPattern = FONT_CHARS[char.uppercaseChar()] ?: return
        for (py in 0 until CHAR_HEIGHT) {
            for (px in 0 until CHAR_WIDTH) {
                if (charPattern[py][px] == 1) {
                    val matrixX = x + px
                    val matrixY = y + py
                    if (matrixX in 0 until MATRIX_SIZE && matrixY in 0 until MATRIX_SIZE) {
                        matrix[matrixY][matrixX] = brightness
                    }
                }
            }
        }
    }

    private fun drawText(matrix: Array<IntArray>, text: String, x: Int, y: Int, brightness: Int = 2047) {
        var xPos = x
        text.uppercase().forEach { char ->
            if (xPos < MATRIX_SIZE) {
                drawChar(matrix, char, xPos, y, brightness)
            }
            xPos += CHAR_WIDTH + CHAR_SPACING
        }
    }

    private fun startScrollingText() {
        scrollJob?.cancel()
        scrollJob = gameScope.launch {
            while (isActive && gameState == GameState.START_SCREEN) {
                startScreenScrollPosition += SCROLL_SPEED
                if (startScreenScrollPosition >= SCROLL_RESET_POSITION) {
                    startScreenScrollPosition = 0f
                }
                renderStartScreen()
                delay(16) // ~60fps
            }
        }
    }

    private fun stopScrollingText() {
        scrollJob?.cancel()
        scrollJob = null
    }

    private fun renderStartScreen() {
        glyphMatrixManager?.let { manager ->
            val matrix = Array(MATRIX_SIZE) { IntArray(MATRIX_SIZE) }

            // Draw static paddles
            // Left paddle (player)
            for (offset in -2..2) {
                val pos = 13 + offset  // Center position
                if (pos in 0 until paddlePositions.size) {
                    val (x, y) = paddlePositions[pos]
                    matrix[y][x] = 2047
                }
            }

            // Right paddle (bot)
            for (offset in -2..2) {
                val pos = 13 + offset  // Center position
                if (pos in 0 until paddlePositions.size) {
                    val (x, y) = paddlePositions[pos]
                    val botX = MATRIX_SIZE - 1 - x
                    matrix[y][botX] = BOT_PADDLE_OPACITY
                }
            }

            // Draw "PONG" text at the top
            drawText(matrix, "PONG", 5, 4)

            // Draw static 2x2 ball in the middle
            val ballX = MATRIX_SIZE / 2 - 1
            val ballY = MATRIX_SIZE / 2 - 1
            for (dy in 0 until 2) {
                for (dx in 0 until 2) {
                    matrix[ballY + dy][ballX + dx] = 2047
                }
            }

            // Draw scrolling text
            val scrollText = "LV ${levelManager.currentLevel} - HI ${levelManager.highestLevel}    "
            val textWidth = scrollText.length * (CHAR_WIDTH + 3)

            // Draw first copy
            val x1 = (SCROLL_START_X - startScreenScrollPosition).toInt()
            drawText(matrix, scrollText, x1, 16)

            // Draw second copy for seamless loop
            val x2 = x1 + textWidth
            if (x2 < MATRIX_SIZE) {
                drawText(matrix, scrollText, x2, 16)
            }

            manager.setMatrixFrame(matrix.flatMap { it.asIterable() }.toIntArray())
        }
    }

    private fun renderGameScreen(matrix: Array<IntArray>) {
        // Draw paddles
        (-2..2).forEach { offset ->
            val pos = pointerIndex + offset
            if (pos in 0 until paddlePositions.size) {
                val (x, y) = paddlePositions[pos]
                matrix[y][x] = 2047
            }
        }

        // Draw bot paddle
        (-2..2).forEach { offset ->
            val pos = botPointerIndex + offset
            if (pos in 0 until paddlePositions.size) {
                val (x, y) = paddlePositions[pos]
                val botX = MATRIX_SIZE - 1 - x
                matrix[y][botX] = BOT_PADDLE_OPACITY
            }
        }

        // Draw ball
        val (ballX, ballY) = ball.getPosition()
        for (offsetX in 0 until BALL_SIZE) {
            for (offsetY in 0 until BALL_SIZE) {
                val x = ballX + offsetX
                val y = ballY + offsetY
                if (x in 0 until MATRIX_SIZE && y in 0 until MATRIX_SIZE) {
                    matrix[y][x] = 2047
                }
            }
        }
    }

    private fun setupSensors(context: Context) {
        sensorManager = context.getSystemService(SensorManager::class.java)
        rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
        sensorManager?.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_GAME)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (gameState != GameState.PLAYING) return

        if (event?.sensor?.type == Sensor.TYPE_GAME_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            SensorManager.getOrientation(rotationMatrix, orientationAngles)

            // Invert pitch and use a smaller range for more precise control
            val pitch = -Math.toDegrees(orientationAngles[1].toDouble()).toFloat()  // Now positive = tilt forward

            // Map pitch from -30 to 30 degrees to paddle position
            val normalizedPitch = (pitch.coerceIn(-30f, 30f) + 30f) / 60f

            // Invert the position mapping for more natural feel (tilt forward = paddle down)
            val targetPosition = ((1f - normalizedPitch) * (paddlePositions.size - 1))
                .coerceIn(0f, (paddlePositions.size - 1).toFloat())

            // Smooth the movement more aggressively at the extremes
            val smoothingFactor = when {
                paddlePosition < 2 || paddlePosition > paddlePositions.size - 3 -> 0.8f
                else -> 0.6f
            }

            paddlePosition = (paddlePosition * smoothingFactor + targetPosition * (1 - smoothingFactor))
                .coerceIn(0f, (paddlePositions.size - 1).toFloat())

            pointerIndex = paddlePosition.roundToInt()

            if (isPlaying) {
                updateDisplay()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for this implementation
    }

    private class Ball {
        var x = MATRIX_SIZE / 2f
        var y = MATRIX_SIZE / 2f
        var velocityX = 0f
        var velocityY = 0f
        private var currentSpeed = INITIAL_BALL_SPEED

        fun reset() {
            x = MATRIX_SIZE / 2f
            y = MATRIX_SIZE / 2f
            velocityX = 0f
            velocityY = 0f
            currentSpeed = INITIAL_BALL_SPEED
        }

        fun start() {
            // Random angle between -30 and 30 degrees for more horizontal initial direction
            val angle = Math.toRadians((Math.random() * 60 - 30).toDouble())
            // Randomly choose left or right initial direction
            val direction = if (Math.random() < 0.5) 1 else -1

            velocityX = (currentSpeed * direction * Math.cos(angle)).toFloat()
            velocityY = (currentSpeed * Math.sin(angle)).toFloat()
            normalizeVelocity()
        }

        fun update(speedMultiplier: Float = 1f) {
            if (velocityX != 0f || velocityY != 0f) {
                // Increase speed gradually
                currentSpeed = (currentSpeed * 1.001f).coerceAtMost(0.5f)

                x += velocityX * speedMultiplier
                y += velocityY * speedMultiplier
                velocityY = velocityY.coerceIn(-0.8f, 0.8f)
            }
        }

        fun reverseX() {
            velocityX = -velocityX
            normalizeVelocity()
        }

        fun reverseY() {
            velocityY = -velocityY
            normalizeVelocity()
        }

        private fun normalizeVelocity() {
            // Calculate current velocity vector magnitude
            val currentMagnitude = Math.sqrt((velocityX * velocityX + velocityY * velocityY).toDouble()).toFloat()

            // Normalize and scale to current speed
            velocityX = (velocityX / currentMagnitude * currentSpeed)
            velocityY = (velocityY / currentMagnitude * currentSpeed)

            // Ensure minimum horizontal velocity to prevent vertical bounces
            if (Math.abs(velocityX) < MIN_X_VELOCITY * currentSpeed) {
                val sign = if (velocityX >= 0) 1 else -1
                velocityX = sign * MIN_X_VELOCITY * currentSpeed
                // Recalculate Y velocity to maintain speed
                val maxY = Math.sqrt((currentSpeed * currentSpeed - velocityX * velocityX).toDouble()).toFloat()
                velocityY = velocityY.coerceIn(-maxY, maxY)
            }
        }

        fun getPosition(): Pair<Int, Int> {
            return Pair(x.toInt(), y.toInt())
        }

        fun getCurrentSpeed(): Float {
            return currentSpeed
        }
    }
}
