package com.gymtracker.ui.manage

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymtracker.data.models.Exercise
import com.gymtracker.data.models.MeasurementType
import com.gymtracker.ui.MainViewModel
import com.gymtracker.ui.theme.*

val CATEGORIES = listOf("Push", "Pull", "Legs", "Core", "Cardio", "Other")

enum class ManageTab { EXERCISES, MEASUREMENTS }

@Composable
fun ManageScreen(viewModel: MainViewModel) {
    val exercises by viewModel.exercises.collectAsState()
    val measurementTypes by viewModel.measurementTypes.collectAsState()

    var tab by remember { mutableStateOf(ManageTab.EXERCISES) }
    var showAddExercise by remember { mutableStateOf(false) }
    var showAddMeasurement by remember { mutableStateOf(false) }
    var editExercise by remember { mutableStateOf<Exercise?>(null) }

    Scaffold(
        containerColor = Surface,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (tab == ManageTab.EXERCISES) showAddExercise = true
                    else showAddMeasurement = true
                },
                containerColor = Neon,
                contentColor = Color.Black
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header
            Text(
                "MANAGE",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = OnSurface,
                letterSpacing = 3.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
            )

            TabRow(
                selectedTabIndex = tab.ordinal,
                containerColor = SurfaceVariant,
                contentColor = Neon,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[tab.ordinal]),
                        color = Neon,
                        height = 2.dp
                    )
                }
            ) {
                Tab(
                    selected = tab == ManageTab.EXERCISES,
                    onClick = { tab = ManageTab.EXERCISES },
                    text = { Text("Exercises (${exercises.size})", color = if (tab == ManageTab.EXERCISES) Neon else SubText) }
                )
                Tab(
                    selected = tab == ManageTab.MEASUREMENTS,
                    onClick = { tab = ManageTab.MEASUREMENTS },
                    text = { Text("Measurements (${measurementTypes.size})", color = if (tab == ManageTab.MEASUREMENTS) Neon else SubText) }
                )
            }

            when (tab) {
                ManageTab.EXERCISES -> ExerciseList(
                    exercises = exercises,
                    onEdit = { editExercise = it },
                    onDelete = { viewModel.deleteExercise(it) }
                )
                ManageTab.MEASUREMENTS -> MeasurementList(
                    types = measurementTypes,
                    onDelete = { viewModel.deleteMeasurementType(it) }
                )
            }
        }
    }

    // Add Exercise Dialog
    if (showAddExercise) {
        ExerciseDialog(
            title = "Add Exercise",
            onDismiss = { showAddExercise = false },
            onConfirm = { name, cat ->
                viewModel.addExercise(name, cat)
                showAddExercise = false
            }
        )
    }

    // Edit Exercise Dialog
    editExercise?.let { ex ->
        ExerciseDialog(
            title = "Edit Exercise",
            initialName = ex.name,
            initialCategory = ex.category,
            onDismiss = { editExercise = null },
            onConfirm = { name, cat ->
                viewModel.updateExercise(ex.id, name, cat)
                editExercise = null
            }
        )
    }

    // Add Measurement Dialog
    if (showAddMeasurement) {
        AddMeasurementDialog(
            onDismiss = { showAddMeasurement = false },
            onConfirm = { name ->
                viewModel.addMeasurementType(name)
                showAddMeasurement = false
            }
        )
    }
}

// ── Exercise List ─────────────────────────────────────────────────────────────

@Composable
fun ExerciseList(exercises: List<Exercise>, onEdit: (Exercise) -> Unit, onDelete: (Int) -> Unit) {
    val grouped = remember(exercises) { exercises.groupBy { it.category }.toSortedMap() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        grouped.forEach { (category, exList) ->
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(categoryColor(category), RoundedCornerShape(4.dp))
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        category.uppercase(),
                        color = categoryColor(category),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                }
            }
            items(exList, key = { it.id }) { ex ->
                ExerciseCard(ex, onEdit, onDelete)
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
fun ExerciseCard(exercise: Exercise, onEdit: (Exercise) -> Unit, onDelete: (Int) -> Unit) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Card),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(exercise.name, color = OnSurface, modifier = Modifier.weight(1f), fontSize = 14.sp)
            IconButton(onClick = { onEdit(exercise) }) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = SubText, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = SubText, modifier = Modifier.size(18.dp))
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = Card,
            title = { Text("Delete \"${exercise.name}\"?", color = OnSurface, fontSize = 16.sp) },
            text = { Text("This cannot be undone.", color = SubText) },
            confirmButton = {
                Button(
                    onClick = { onDelete(exercise.id); showDeleteConfirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel", color = SubText) }
            }
        )
    }
}

// ── Measurement Type List ─────────────────────────────────────────────────────

@Composable
fun MeasurementList(types: List<MeasurementType>, onDelete: (Int) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(types, key = { it.id }) { mt ->
            MeasurementCard(mt, onDelete)
        }
    }
}

@Composable
fun MeasurementCard(type: MeasurementType, onDelete: (Int) -> Unit) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Card),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Straighten, contentDescription = null, tint = Color(0xFF4ECDC4), modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(12.dp))
            Text(type.name, color = OnSurface, modifier = Modifier.weight(1f), fontSize = 14.sp)
            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = SubText, modifier = Modifier.size(18.dp))
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = Card,
            title = { Text("Delete \"${type.name}\"?", color = OnSurface, fontSize = 16.sp) },
            text = { Text("This cannot be undone.", color = SubText) },
            confirmButton = {
                Button(
                    onClick = { onDelete(type.id); showDeleteConfirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel", color = SubText) }
            }
        )
    }
}

// ── Dialogs ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDialog(
    title: String,
    initialName: String = "",
    initialCategory: String = "Push",
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var category by remember { mutableStateOf(initialCategory) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Card,
        title = { Text(title, color = Neon, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Exercise Name", color = SubText) },
                    colors = com.gymtracker.ui.home.gymTextFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category", color = SubText) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        colors = com.gymtracker.ui.home.gymTextFieldColors(),
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(CardElevated)
                    ) {
                        CATEGORIES.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat, color = OnSurface) },
                                onClick = { category = cat; expanded = false }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim(), category) },
                colors = ButtonDefaults.buttonColors(containerColor = Neon, contentColor = Color.Black),
                enabled = name.isNotBlank()
            ) { Text("Save", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = SubText) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMeasurementDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Card,
        title = { Text("Add Measurement Type", color = Color(0xFF4ECDC4), fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name (e.g. Thigh)", color = SubText) },
                colors = com.gymtracker.ui.home.gymTextFieldColors(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4ECDC4), contentColor = Color.Black),
                enabled = name.isNotBlank()
            ) { Text("Add", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = SubText) }
        }
    )
}
