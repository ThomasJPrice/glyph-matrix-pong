                               package com.thomasp.pong.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsernameDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Enter Username",
                    style = MaterialTheme.typography.titleLarge
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it.take(15) // Limit to 15 characters
                        showError = false
                    },
                    label = { Text("Username") },
                    isError = showError,
                    supportingText = if (showError) {
                        { Text("Username must be at least 3 characters") }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (username.length >= 3) {
                                onConfirm(username)
                            } else {
                                showError = true
                            }
                        }
                    ) {
                        Text("Confirm")
                    }
                }
            }
        }
    }
}
