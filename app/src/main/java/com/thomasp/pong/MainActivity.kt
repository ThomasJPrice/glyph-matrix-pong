package com.thomasjprice.pong

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.activity.compose.BackHandler
import androidx.compose.ui.text.style.TextAlign
import com.thomasjprice.pong.ui.theme.PongTypography
import com.thomasjprice.pong.api.LeaderboardService
import com.thomasjprice.pong.data.UserPreferences
import com.thomasjprice.pong.ui.LeaderboardScreen
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var leaderboardService: LeaderboardService
    private lateinit var userPreferences: UserPreferences
    private val mainScope = MainScope()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        leaderboardService = LeaderboardService(this)
        userPreferences = UserPreferences(this)
        enableEdgeToEdge()

        // Initialize username in background (no dialog, no loading screen)
        mainScope.launch {
            leaderboardService.ensureUsername()
        }

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color.White,
                    onPrimary = Color.Black,
                    surface = Color(0xFF141414),
                    onSurface = Color.White,
                    background = Color(0xFF141414),
                    onBackground = Color.White
                ),
                typography = PongTypography
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var showLeaderboard by remember { mutableStateOf(false) }

                    // Handle system back button
                    BackHandler(enabled = showLeaderboard) {
                        showLeaderboard = false
                    }

                    if (showLeaderboard) {
                        LeaderboardScreen(
                            leaderboardService = leaderboardService,
                            onBack = { showLeaderboard = false }
                        )
                    } else {
                        MainScreen(
                            onLeaderboardClick = { showLeaderboard = true }
                        )
                    }
                }
            }
        }
    }
}

class PongSettings {
    companion object {
        var soundEnabled by mutableStateOf(true)
        var hapticEnabled by mutableStateOf(true)
        var howToPlayExpanded by mutableStateOf(false)
        var installationExpanded by mutableStateOf(false)
    }
}

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    onLeaderboardClick: () -> Unit = {}
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Title
            Text(
                text = "Pong",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 40.dp)
            )

            Image(
                painter = painterResource(R.drawable.pong_thumbnail),
                contentDescription = "Pong Preview",
                modifier = Modifier.size(200.dp)
            )

            // Buttons section
            Button(
                onClick = onLeaderboardClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Leaderboard")
            }

            // Settings List
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Sound Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 0.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sound",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Switch(
                            checked = PongSettings.soundEnabled,
                            onCheckedChange = { PongSettings.soundEnabled = it }
                        )
                    }

//                HorizontalDivider(
//                    color = Color.Gray.copy(alpha = 0.2f),
//                    thickness = 1.dp
//                )

                    // Haptic Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 0.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Haptic Vibration",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Switch(
                            checked = PongSettings.hapticEnabled,
                            onCheckedChange = { PongSettings.hapticEnabled = it }
                        )
                    }
                }
            }

            // How to Use Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "How to Use",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                // Installation Section
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { PongSettings.installationExpanded = !PongSettings.installationExpanded },
                    border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f)),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Installation",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Icon(
                                imageVector = if (PongSettings.installationExpanded) {
                                    Icons.Default.KeyboardArrowUp
                                } else {
                                    Icons.Default.KeyboardArrowDown
                                },
                                contentDescription = "Expand"
                            )
                        }

                        AnimatedVisibility(visible = PongSettings.installationExpanded) {
                            Column(
                                modifier = Modifier.padding(
                                    start = 16.dp,
                                    end = 16.dp,
                                    bottom = 16.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                HorizontalDivider(
                                    color = Color.Gray.copy(alpha = 0.3f),
                                    thickness = 1.dp
                                )
                                Text("• Settings > Glyph Interface > Glyph Toys")
                                Text("• Select reorder icon (top right)")
                                Text("• Drag Pong to be an active toy")
                            }
                        }
                    }
                }

                // Gameplay Section
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { PongSettings.howToPlayExpanded = !PongSettings.howToPlayExpanded },
                    border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f)),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Gameplay",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Icon(
                                imageVector = if (PongSettings.howToPlayExpanded) {
                                    Icons.Default.KeyboardArrowUp
                                } else {
                                    Icons.Default.KeyboardArrowDown
                                },
                                contentDescription = "Expand"
                            )
                        }

                        AnimatedVisibility(visible = PongSettings.howToPlayExpanded) {
                            Column(
                                modifier = Modifier.padding(
                                    start = 16.dp,
                                    end = 16.dp,
                                    bottom = 16.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                HorizontalDivider(
                                    color = Color.Gray.copy(alpha = 0.3f),
                                    thickness = 1.dp
                                )
                                Text("• Long press to start Pong")
                                Text("• Tilt phone to control paddle")
                                Text("• Avoid missing the ball and try to beat the bot")
                                Text("• If you score, you will level up and it gets harder")
                                Text("• When the bot beats you, back to level one")
                            }
                        }
                    }
                }
            }

            // Attribution
            Text(
                text = "v1.1\n" +
                        "Made by Thomas",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 24.dp).fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}
