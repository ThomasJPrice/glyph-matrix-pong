package com.thomasp.pong.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.thomasp.pong.api.LeaderboardResponse
import com.thomasp.pong.api.LeaderboardService
import kotlinx.coroutines.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    leaderboardService: LeaderboardService,
    onBack: () -> Unit
) {
    // Define medal colors that work well with dark theme
    val goldColor = Color(0xFF665728)    // Darker shade of gold
    val silverColor = Color(0xFF4A4A54)  // Darker shade of silver
    val bronzeColor = Color(0xFF614434)  // Darker shade of bronze

    var leaderboardState by remember { mutableStateOf<LeaderboardResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Username dialog state
    var showUsernameDialog by remember { mutableStateOf(false) }
    var currentUsername by remember { mutableStateOf<String?>(null) }

    // Snackbar host state for showing messages
    val snackbarHostState = remember { SnackbarHostState() }

    // Load username and ensure it exists
    LaunchedEffect(Unit) {
        scope.launch {
            currentUsername = leaderboardService.ensureUsername()
        }
    }

    LaunchedEffect(Unit) {
        scope.launch {
            isLoading = true
            try {
                leaderboardService.getLeaderboard()
                    .onSuccess {
                        leaderboardState = it
                        error = null
                    }
                    .onFailure { e ->
                        error = "Failed to load leaderboard: ${e.message}"
                        android.util.Log.e("LeaderboardScreen", "Error loading leaderboard", e)
                    }
            } catch (e: Exception) {
                error = "Unexpected error: ${e.message}"
                android.util.Log.e("LeaderboardScreen", "Unexpected error", e)
            }
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Leaderboard") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showUsernameDialog = true }) {
                        Icon(Icons.Default.Edit, "Edit Username")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {

            when {
                isLoading -> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        CircularProgressIndicator()
                    }
                }
                error != null -> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = error!!,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                leaderboardState != null -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(leaderboardState!!.top) { index, entry ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = when {
                                        entry.user_id == leaderboardService.getUserId() -> MaterialTheme.colorScheme.primaryContainer
                                        index == 0 -> goldColor
                                        index == 1 -> silverColor
                                        index == 2 -> bronzeColor
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "#${index + 1}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = entry.username,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                    Text(
                                        text = entry.score.toString(),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Show user's position if not in top 10
                    if (!leaderboardState!!.inTop && leaderboardState!!.position != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Your Position: #${leaderboardState!!.position}",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = leaderboardState!!.score.toString(),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        UsernameDialog(
            leaderboardService = leaderboardService,
            currentUsername = currentUsername,
            isOpen = showUsernameDialog,
            onDismiss = { showUsernameDialog = false },
            onSuccess = { newUsername ->
                currentUsername = newUsername
                showUsernameDialog = false
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Username updated successfully",
                        duration = SnackbarDuration.Short
                    )
                }
            },
            onError = { errorMessage ->
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = errorMessage,
                        duration = SnackbarDuration.Long
                    )
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun UsernameDialog(
    leaderboardService: LeaderboardService,
    currentUsername: String?,
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
) {
    var username by remember(currentUsername) { mutableStateOf(currentUsername ?: "") }
    var isChecking by remember { mutableStateOf(false) }
    var isAvailable by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    var checkJob by remember { mutableStateOf<Job?>(null) }

    // Check username availability with debounce
    LaunchedEffect(username) {
        if (username.length >= 3 && username != currentUsername) {
            // Cancel any existing check
            checkJob?.cancel()

            // Only show checking state if the check takes longer than 500ms
            val showLoadingJob = launch {
                delay(500)
                isChecking = true
            }

            checkJob = launch {
                delay(600) // Debounce delay
                try {
                    leaderboardService.checkUsernameAvailability(username)
                        .onSuccess { available ->
                            isAvailable = available
                            errorMessage = if (!available) "Username is already taken" else null
                        }
                        .onFailure {
                            // Don't show network errors while typing
                            isAvailable = false
                        }
                } finally {
                    showLoadingJob.cancel()
                    isChecking = false
                }
            }
        } else {
            checkJob?.cancel()
            isChecking = false
            // Only show validation errors for short usernames
            errorMessage = when {
                username.isEmpty() -> null
                username.length < 3 -> "Username must be at least 3 characters"
                username.length > 20 -> "Username must be less than 20 characters"
                !username.matches(Regex("^[a-zA-Z0-9_]+$")) ->
                    "Only letters, numbers, and underscores allowed"
                else -> null
            }
        }
    }

    if (isOpen) {
        AlertDialog(
            onDismissRequest = { if (!isSaving) onDismiss() },
            title = { Text("Set Username") },
            text = {
                Column {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { newUsername ->
                            username = newUsername
                            errorMessage = when {
                                newUsername.length < 3 -> "Username must be at least 3 characters"
                                newUsername.length > 20 -> "Username must be less than 20 characters"
                                !newUsername.matches(Regex("^[a-zA-Z0-9_]+$")) ->
                                    "Only letters, numbers, and underscores allowed"
                                else -> null
                            }
                        },
                        label = { Text("Username") },
                        singleLine = true,
                        isError = errorMessage != null,
                        enabled = !isSaving,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = if (isChecking) {
                            {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        } else null
                    )
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            isSaving = true
                            try {
                                leaderboardService.setUsername(username)
                                    .onSuccess {
                                        onSuccess(username)
                                    }
                                    .onFailure { e ->
                                        onError(e.message ?: "Failed to update username")
                                    }
                            } catch (e: Exception) {
                                onError("Network error while updating username")
                            } finally {
                                isSaving = false
                            }
                        }
                    },
                    enabled = errorMessage == null &&
                             username.isNotEmpty() &&
                             !isChecking &&
                             !isSaving &&
                             (username == currentUsername || isAvailable)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Save")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismiss,
                    enabled = !isSaving
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
