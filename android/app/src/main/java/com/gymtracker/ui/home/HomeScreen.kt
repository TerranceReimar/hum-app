package com.gymtracker.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymtracker.data.models.Exercise
import com.gymtracker.data.models.MeasurementType
import com.gymtracker.ui.MainViewModel
import com.gymtracker.ui.theme.*

@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val exercises by viewModel.exercises.collectAsState()
    val measurementTypes by viewModel.measurementTypes.collectAsState()

    var showWorkoutDialog by remember { mutableStateOf(false) }
    var showMetricsDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                text = "HUM",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = Neon,
                letterSpacing = 4.sp
            )
            Text(
                text = "HIGH-SPEED LOGGING",
                fontSize = 11.sp,
                color = SubText,
                letterSpacing = 3.sp
            )

            Spacer(Modifier.height(60.dp))

            // Quick Log Buttons
            QuickLogButton(
                label = "LOG WORKOUT",
                icon = Icons.Default.FitnessCenter,
                gradient = Brush.horizontalGradient(listOf(Color(0xFF1A1A1A), Color(0xFF2A3300))),
                accentColor = Neon,
                onClick = { showWorkoutDialog = true }
            )

            Spacer(Modifier.height(20.dp))

            QuickLogButton(
                label = "LOG METRICS",
                icon = Icons.Default.MonitorWeight,
                gradient = Brush.horizontalGradient(listOf(Color(0xFF1A1A1A), Color(0xFF003329))),
                accentColor = Color(0xFF4ECDC4),
                onClick = { showMetricsDialog = true }
            )

            Spacer(Modifier.height(60.dp))

            // Quick stats
            Text(
                text = "${exercises.size} exercises · ${measurementTypes.size} metrics tracked",
                fontSize = 12.sp,
                color = SubText
            )
        }
    }

    if (showWorkoutDialog) {
        WorkoutLogDialog(
            exercises = exercises,
            onDismiss = { showWorkoutDialog = false },
            onLog = { exercise, sets, reps, weight, tempo ->
                viewModel.logWorkout(exercise, sets, reps, weight, tempo)
                showWorkoutDialog = false
            }
        )
    }

    if (showMetricsDialog) {
        MetricsLogDialog(
            measurementTypes = measurementTypes,
            onDismiss = { showMetricsDialog = false },
            onLog = { type, value, notes ->
                viewModel.logMetric(type, value, notes)
                showMetricsDialog = false
            }
        )
    }
}

@Composable
private fun QuickLogButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    gradient: Brush,
    accentColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(gradient)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accentColor,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = label,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                letterSpacing = 2.sp
            )
        }
        // Border
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(accentColor.copy(alpha = 0.3f), Color.Transparent)
                    )
                )
        )
    }
}

// ── Workout Log Dialog ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutLogDialog(
    exercises: List<Exercise>,
    onDismiss: () -> Unit,
    onLog: (String, Int, Int, Float, String) -> Unit
) {
    var selectedExercise by remember { mutableStateOf<Exercise?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showDropdown by remember { mutableStateOf(false) }
    var sets by remember { mutableStateOf("3") }
    var reps by remember { mutableStateOf("10") }
    var weight by remember { mutableStateOf("") }
    var tempo by remember { mutableStateOf("") }

    val filteredExercises = remember(searchQuery, exercises) {
        if (searchQuery.isBlank()) exercises
        else exercises.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Card,
        title = {
            Text("Log Workout", color = Neon, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Exercise search
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        showDropdown = true
                        if (selectedExercise?.name != it) selectedExercise = null
                    },
                    label = { Text("Exercise", color = SubText) },
                    placeholder = { Text("Search exercises…", color = SubText) },
                    trailingIcon = {
                        Icon(
                            Icons.Default.ArrowDropDown,
                            null,
                            tint = Neon,
                            modifier = Modifier.clickable { showDropdown = !showDropdown }
                        )
                    },
                    colors = gymTextFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                AnimatedVisibility(visible = showDropdown && filteredExercises.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardElevated),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        LazyColumn(modifier = Modifier.heightIn(max = 180.dp)) {
                            items(filteredExercises) { ex ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedExercise = ex
                                            searchQuery = ex.name
                                            showDropdown = false
                                        }
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(categoryColor(ex.category), shape = RoundedCornerShape(4.dp))
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(ex.name, color = OnSurface, fontSize = 14.sp)
                                        Text(ex.category, color = SubText, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // Sets / Reps row
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = sets,
                        onValueChange = { sets = it },
                        label = { Text("Sets", color = SubText) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = gymTextFieldColors(),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = reps,
                        onValueChange = { reps = it },
                        label = { Text("Reps", color = SubText) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = gymTextFieldColors(),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                // Weight / Tempo row
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { weight = it },
                        label = { Text("Weight (kg)", color = SubText) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = gymTextFieldColors(),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = tempo,
                        onValueChange = { tempo = it },
                        label = { Text("Tempo", color = SubText) },
                        placeholder = { Text("4-0-1-0", color = SubText) },
                        colors = gymTextFieldColors(),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val exerciseName = selectedExercise?.name ?: searchQuery.trim()
                    if (exerciseName.isBlank()) return@Button
                    onLog(
                        exerciseName,
                        sets.toIntOrNull() ?: 3,
                        reps.toIntOrNull() ?: 10,
                        weight.toFloatOrNull() ?: 0f,
                        tempo
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Neon, contentColor = Color.Black),
                enabled = (selectedExercise != null || searchQuery.isNotBlank()) && weight.isNotBlank()
            ) {
                Icon(Icons.Default.Upload, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("LOG IT", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = SubText) }
        }
    )
}

// ── Metrics Log Dialog ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetricsLogDialog(
    measurementTypes: List<MeasurementType>,
    onDismiss: () -> Unit,
    onLog: (String, Float, String) -> Unit
) {
    // Map of metricType -> value string
    val values = remember { mutableStateMapOf<String, String>() }
    var notes by remember { mutableStateOf("") }

    // Prioritise common ones at top
    val ordered = remember(measurementTypes) {
        val priority = listOf("Weight", "Waist (Navel)", "Arm (Flexed)")
        val top = priority.mapNotNull { name -> measurementTypes.find { it.name == name } }
        val rest = measurementTypes.filter { mt -> top.none { it.id == mt.id } }
        top + rest
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Card,
        title = {
            Text("Log Metrics", color = Color(0xFF4ECDC4), fontWeight = FontWeight.Bold, fontSize = 20.sp)
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(ordered) { mt ->
                    OutlinedTextField(
                        value = values[mt.name] ?: "",
                        onValueChange = { values[mt.name] = it },
                        label = { Text(mt.name, color = SubText) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = gymTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        trailingIcon = {
                            if (mt.name.lowercase().contains("weight")) Text("kg", color = SubText, fontSize = 12.sp)
                            else Text("cm", color = SubText, fontSize = 12.sp)
                        }
                    )
                }
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes (optional)", color = SubText) },
                        colors = gymTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    values.forEach { (type, rawVal) ->
                        val v = rawVal.toFloatOrNull() ?: return@forEach
                        onLog(type, v, notes)
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4ECDC4),
                    contentColor = Color.Black
                ),
                enabled = values.values.any { it.toFloatOrNull() != null }
            ) {
                Icon(Icons.Default.Upload, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("SYNC", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = SubText) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun gymTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Neon,
    unfocusedBorderColor = Color(0xFF333333),
    focusedTextColor = OnSurface,
    unfocusedTextColor = OnSurface,
    cursorColor = Neon,
    focusedContainerColor = SurfaceVariant,
    unfocusedContainerColor = SurfaceVariant
)
