package com.gymtracker.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymtracker.data.api.RetrofitClient
import com.gymtracker.ui.MainViewModel
import com.gymtracker.ui.home.gymTextFieldColors
import com.gymtracker.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val scope = rememberCoroutineScope()
    val currentUrl by viewModel.baseUrl.collectAsState()

    var urlInput by remember(currentUrl) { mutableStateOf(currentUrl) }
    var connectionStatus by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface)
            .padding(20.dp)
    ) {
        Text(
            "SETTINGS",
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            color = OnSurface,
            letterSpacing = 3.sp
        )

        Spacer(Modifier.height(32.dp))

        // Connection Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Card),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Wifi, null, tint = Neon, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Raspberry Pi Connection", color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = { Text("Base URL", color = SubText) },
                    placeholder = { Text("http://192.168.1.100:8000", color = SubText) },
                    colors = gymTextFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Link, null, tint = SubText, modifier = Modifier.size(18.dp))
                    }
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    "Find your Pi's IP: run 'hostname -I' on the Pi",
                    color = SubText,
                    fontSize = 11.sp
                )

                Spacer(Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Test Connection
                    OutlinedButton(
                        onClick = {
                            isTesting = true
                            connectionStatus = null
                            scope.launch {
                                try {
                                    RetrofitClient.setBaseUrl(urlInput)
                                    val resp = RetrofitClient.service.health()
                                    connectionStatus = if (resp.isSuccessful) "✅ Connected!" else "❌ Error ${resp.code()}"
                                } catch (e: Exception) {
                                    connectionStatus = "❌ ${e.message?.take(50)}"
                                }
                                isTesting = false
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Neon),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            // tint via content color
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Neon, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.NetworkCheck, null, modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(6.dp))
                        Text("Test")
                    }

                    // Save
                    Button(
                        onClick = {
                            viewModel.saveBaseUrl(urlInput.trim())
                            connectionStatus = "Saved!"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Neon, contentColor = Color.Black),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Save, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }

                connectionStatus?.let { status ->
                    Spacer(Modifier.height(12.dp))
                    Text(
                        status,
                        color = if (status.startsWith("✅") || status == "Saved!") Neon else ErrorRed,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Info Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Card),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, tint = SubText, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Quick Setup", color = OnSurface, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))

                val steps = listOf(
                    "1. SSH into your Raspberry Pi",
                    "2. Clone the repo and cd into backend/",
                    "3. Run: docker-compose up -d",
                    "4. Note the Pi's local IP from router or 'hostname -I'",
                    "5. Enter http://<PI_IP>:8000 above and save"
                )
                steps.forEach { step ->
                    Text(step, color = SubText, fontSize = 12.sp, modifier = Modifier.padding(vertical = 2.dp))
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // App info
        Text(
            "GymTracker v1.0 · Built for the grind",
            color = SubText,
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}
