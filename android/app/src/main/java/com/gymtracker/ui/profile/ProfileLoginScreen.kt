package com.gymtracker.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymtracker.data.api.RetrofitClient
import com.gymtracker.data.models.Profile
import com.gymtracker.ui.MainViewModel
import com.gymtracker.ui.theme.*
import kotlinx.coroutines.launch
import androidx.compose.foundation.text.KeyboardOptions

@Composable
fun ProfileLoginScreen(viewModel: MainViewModel) {
    val profiles by viewModel.profiles.collectAsState()
    val isLoading by viewModel.isLoadingProfile.collectAsState()
    val currentUrl by viewModel.baseUrl.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var newProfileName by remember { mutableStateOf("") }
    var newProfilePassword by remember { mutableStateOf("") }
    var newProfilePasswordConfirm by remember { mutableStateOf("") }
    var showServerDialog by remember { mutableStateOf(false) }

    var pendingProfile by remember { mutableStateOf<Profile?>(null) }
    var showPasswordEntryDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface)
    ) {
        // Gear button — always reachable in top-right
        IconButton(
            onClick = { showServerDialog = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
        ) {
            Icon(Icons.Default.Settings, contentDescription = "Server settings", tint = SubText)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(64.dp))

            Icon(
                Icons.Default.FitnessCenter,
                contentDescription = null,
                tint = Neon,
                modifier = Modifier.size(56.dp)
            )

            Spacer(Modifier.height(16.dp))

            Text(
                "HUM",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = Neon,
                letterSpacing = 4.sp
            )

            Text(
                "Select your profile",
                fontSize = 14.sp,
                color = SubText,
                letterSpacing = 1.sp
            )

            Spacer(Modifier.height(48.dp))

            if (isLoading) {
                CircularProgressIndicator(color = Neon)
            } else if (profiles.isEmpty()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "No profiles found.",
                        color = OnSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Tap  ⚙  to configure your server URL,\nthen refresh or create a profile.",
                        color = SubText,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        currentUrl,
                        color = SubText,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(profiles) { profile ->
                        ProfileCard(
                            profile = profile,
                            onClick = {
                                pendingProfile = profile
                                showPasswordEntryDialog = true
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { showCreateDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Neon, contentColor = Color.Black),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Create New Profile", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(Modifier.height(16.dp))

            TextButton(onClick = { viewModel.loadProfiles() }) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = SubText, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Refresh", color = SubText, fontSize = 13.sp)
            }
        }
    }

    // Create profile dialog
    if (showCreateDialog) {
        val passwordMismatch = newProfilePasswordConfirm.isNotEmpty() &&
                newProfilePassword != newProfilePasswordConfirm

        AlertDialog(
            onDismissRequest = {
                showCreateDialog = false
                newProfileName = ""
                newProfilePassword = ""
                newProfilePasswordConfirm = ""
            },
            containerColor = Card,
            title = { Text("New Profile", color = OnSurface, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newProfileName,
                        onValueChange = { newProfileName = it },
                        label = { Text("Profile name", color = SubText) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Neon,
                            unfocusedBorderColor = SubText,
                            focusedTextColor = OnSurface,
                            unfocusedTextColor = OnSurface,
                            cursorColor = Neon
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newProfilePassword,
                        onValueChange = { newProfilePassword = it },
                        label = { Text("Password", color = SubText) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Neon,
                            unfocusedBorderColor = SubText,
                            focusedTextColor = OnSurface,
                            unfocusedTextColor = OnSurface,
                            cursorColor = Neon
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newProfilePasswordConfirm,
                        onValueChange = { newProfilePasswordConfirm = it },
                        label = { Text("Confirm password", color = SubText) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        isError = passwordMismatch,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (passwordMismatch) ErrorRed else Neon,
                            unfocusedBorderColor = if (passwordMismatch) ErrorRed else SubText,
                            focusedTextColor = OnSurface,
                            unfocusedTextColor = OnSurface,
                            cursorColor = Neon
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (passwordMismatch) {
                        Text("Passwords do not match", color = ErrorRed, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newProfileName.isNotBlank() && newProfilePassword.isNotBlank() &&
                            newProfilePassword == newProfilePasswordConfirm
                        ) {
                            viewModel.createProfile(newProfileName.trim(), newProfilePassword)
                            showCreateDialog = false
                            newProfileName = ""
                            newProfilePassword = ""
                            newProfilePasswordConfirm = ""
                        }
                    },
                    enabled = newProfileName.isNotBlank() && newProfilePassword.isNotBlank() &&
                            newProfilePassword == newProfilePasswordConfirm
                ) {
                    Text("Create", color = Neon, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCreateDialog = false
                    newProfileName = ""
                    newProfilePassword = ""
                    newProfilePasswordConfirm = ""
                }) {
                    Text("Cancel", color = SubText)
                }
            }
        )
    }

    // Password entry dialog (for existing profiles)
    if (showPasswordEntryDialog && pendingProfile != null) {
        PasswordEntryDialog(
            profileName = pendingProfile!!.name,
            onDismiss = {
                showPasswordEntryDialog = false
                pendingProfile = null
            },
            onVerify = { password, onResult ->
                viewModel.verifyProfilePassword(pendingProfile!!.id, password) { valid ->
                    onResult(valid)
                    if (valid) {
                        viewModel.selectProfile(pendingProfile!!.id)
                        showPasswordEntryDialog = false
                        pendingProfile = null
                    }
                }
            }
        )
    }

    // Server config dialog
    if (showServerDialog) {
        ServerConfigDialog(
            currentUrl = currentUrl,
            onDismiss = { showServerDialog = false },
            onSave = { url ->
                viewModel.saveBaseUrl(url)
                showServerDialog = false
                viewModel.loadProfiles()
            }
        )
    }
}

@Composable
private fun PasswordEntryDialog(
    profileName: String,
    onDismiss: () -> Unit,
    onVerify: (password: String, onResult: (Boolean) -> Unit) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var isVerifying by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Card,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, null, tint = Neon, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(profileName, color = OnSurface, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; showError = false },
                    label = { Text("Password", color = SubText) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    isError = showError,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (showError) ErrorRed else Neon,
                        unfocusedBorderColor = if (showError) ErrorRed else SubText,
                        focusedTextColor = OnSurface,
                        unfocusedTextColor = OnSurface,
                        cursorColor = Neon
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                if (showError) {
                    Spacer(Modifier.height(8.dp))
                    Text("Incorrect password", color = ErrorRed, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isVerifying = true
                    showError = false
                    onVerify(password) { valid ->
                        isVerifying = false
                        if (!valid) showError = true
                    }
                },
                enabled = password.isNotBlank() && !isVerifying,
                colors = ButtonDefaults.buttonColors(containerColor = Neon, contentColor = Color.Black)
            ) {
                if (isVerifying) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                } else {
                    Text("Login", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = SubText) }
        }
    )
}

@Composable
private fun ServerConfigDialog(
    currentUrl: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var urlInput by remember(currentUrl) { mutableStateOf(currentUrl) }
    var status by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Card,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Wifi, null, tint = Neon, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Server Connection", color = OnSurface, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it; status = null },
                    label = { Text("Base URL", color = SubText) },
                    placeholder = { Text("http://192.168.1.100:8000", color = SubText) },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Link, null, tint = SubText, modifier = Modifier.size(18.dp))
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Neon,
                        unfocusedBorderColor = SubText,
                        focusedTextColor = OnSurface,
                        unfocusedTextColor = OnSurface,
                        cursorColor = Neon
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    "Find your Pi's IP: run 'hostname -I' on the Pi",
                    color = SubText,
                    fontSize = 11.sp
                )

                Spacer(Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        isTesting = true
                        status = null
                        scope.launch {
                            try {
                                RetrofitClient.setBaseUrl(urlInput.trim())
                                val resp = RetrofitClient.service.health()
                                status = if (resp.isSuccessful) "✅ Connected!" else "❌ Error ${resp.code()}"
                            } catch (e: Exception) {
                                status = "❌ ${e.message?.take(60)}"
                            }
                            isTesting = false
                        }
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Neon),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Neon, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Testing…")
                    } else {
                        Icon(Icons.Default.NetworkCheck, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Test Connection")
                    }
                }

                status?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        it,
                        color = if (it.startsWith("✅")) Neon else ErrorRed,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(urlInput.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = Neon, contentColor = Color.Black)
            ) {
                Icon(Icons.Default.Save, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = SubText)
            }
        }
    )
}

@Composable
private fun ProfileCard(profile: Profile, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Card),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                tint = Neon,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(profile.name, color = OnSurface, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                profile.currentWeight?.let {
                    Text("$it lbs", color = SubText, fontSize = 12.sp)
                }
            }
            if (profile.hasPassword) {
                Icon(Icons.Default.Lock, contentDescription = "Password protected", tint = SubText, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = SubText)
        }
    }
}
