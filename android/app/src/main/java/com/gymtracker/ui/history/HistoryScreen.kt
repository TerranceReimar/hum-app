package com.gymtracker.ui.history

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymtracker.data.models.UserMetric
import com.gymtracker.data.models.Workout
import com.gymtracker.ui.MainViewModel
import com.gymtracker.ui.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class HistoryTab { WORKOUTS, METRICS }

@Composable
fun HistoryScreen(viewModel: MainViewModel) {
    val workouts by viewModel.workouts.collectAsState()
    val metrics by viewModel.metrics.collectAsState()

    var tab by remember { mutableStateOf(HistoryTab.WORKOUTS) }
    var showChart by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "HISTORY",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = OnSurface,
                letterSpacing = 3.sp,
                modifier = Modifier.weight(1f)
            )
            if (tab == HistoryTab.METRICS) {
                IconButton(onClick = { showChart = !showChart }) {
                    Icon(
                        if (showChart) Icons.Default.TableChart else Icons.Default.ShowChart,
                        contentDescription = "Toggle Chart",
                        tint = if (showChart) Neon else SubText
                    )
                }
            }
            IconButton(onClick = {
                if (tab == HistoryTab.WORKOUTS) viewModel.loadWorkouts()
                else viewModel.loadMetrics()
            }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = SubText)
            }
        }

        // Tab Row
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
                selected = tab == HistoryTab.WORKOUTS,
                onClick = { tab = HistoryTab.WORKOUTS },
                text = {
                    Text(
                        "Workouts (${workouts.size})",
                        color = if (tab == HistoryTab.WORKOUTS) Neon else SubText
                    )
                }
            )
            Tab(
                selected = tab == HistoryTab.METRICS,
                onClick = { tab = HistoryTab.METRICS },
                text = {
                    Text(
                        "Metrics (${metrics.size})",
                        color = if (tab == HistoryTab.METRICS) Color(0xFF4ECDC4) else SubText
                    )
                }
            )
        }

        // Content
        when (tab) {
            HistoryTab.WORKOUTS -> WorkoutHistory(workouts, onDelete = { viewModel.deleteWorkout(it) })
            HistoryTab.METRICS -> MetricsHistory(
                metrics = metrics,
                showChart = showChart,
                onDelete = { viewModel.deleteMetric(it) }
            )
        }
    }
}

// ── Workout History ───────────────────────────────────────────────────────────

@Composable
fun WorkoutHistory(workouts: List<Workout>, onDelete: (Int) -> Unit) {
    if (workouts.isEmpty()) {
        EmptyState("No workouts logged yet.\nHit the home screen to start!")
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(workouts, key = { it.id }) { w ->
            WorkoutCard(w, onDelete)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutCard(workout: Workout, onDelete: (Int) -> Unit) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Card),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left accent bar
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(56.dp)
                    .background(Neon, RoundedCornerShape(2.dp))
            )
            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    workout.exerciseName,
                    fontWeight = FontWeight.Bold,
                    color = OnSurface,
                    fontSize = 15.sp
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatChip("${workout.sets}×${workout.reps}", Neon)
                    StatChip("${workout.weight}kg", Color(0xFFAAAAAA))
                    if (workout.tempo.isNotBlank()) StatChip(workout.tempo, SubText)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    formatTimestamp(workout.timestamp),
                    color = SubText,
                    fontSize = 11.sp
                )
            }

            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = SubText)
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = Card,
            title = { Text("Delete?", color = OnSurface) },
            text = { Text("Remove this workout entry?", color = SubText) },
            confirmButton = {
                Button(
                    onClick = { onDelete(workout.id); showDeleteConfirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel", color = SubText) }
            }
        )
    }
}

// ── Metrics History ───────────────────────────────────────────────────────────

@Composable
fun MetricsHistory(metrics: List<UserMetric>, showChart: Boolean, onDelete: (Int) -> Unit) {
    if (metrics.isEmpty()) {
        EmptyState("No metrics logged yet.")
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (showChart) {
            SimpleLineChart(metrics = metrics)
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(metrics, key = { it.id }) { m ->
                MetricCard(m, onDelete)
            }
        }
    }
}

@Composable
fun MetricCard(metric: UserMetric, onDelete: (Int) -> Unit) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Card),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(44.dp)
                    .background(Color(0xFF4ECDC4), RoundedCornerShape(2.dp))
            )
            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(metric.metricType, color = OnSurface, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "${metric.value}",
                        color = Color(0xFF4ECDC4),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                if (metric.notes.isNotBlank()) {
                    Text(metric.notes, color = SubText, fontSize = 11.sp)
                }
                Text(formatTimestamp(metric.timestamp), color = SubText, fontSize = 11.sp)
            }

            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = SubText)
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = Card,
            title = { Text("Delete?", color = OnSurface) },
            text = { Text("Remove this metric entry?", color = SubText) },
            confirmButton = {
                Button(
                    onClick = { onDelete(metric.id); showDeleteConfirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel", color = SubText) }
            }
        )
    }
}

// ── Simple Chart (using Canvas) ───────────────────────────────────────────────

@Composable
fun SimpleLineChart(metrics: List<UserMetric>) {
    val targetTypes = listOf("Weight", "Waist (Navel)")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        targetTypes.forEach { type ->
            val data = metrics
                .filter { it.metricType == type }
                .sortedBy { it.timestamp }
                .takeLast(20)

            if (data.size >= 2) {
                Text(type, color = Color(0xFF4ECDC4), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                MiniLineChart(data)
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun MiniLineChart(data: List<UserMetric>) {
    val min = data.minOf { it.value }
    val max = data.maxOf { it.value }
    val range = if (max == min) 1f else max - min

    androidx.compose.foundation.Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(CardElevated)
            .padding(8.dp)
    ) {
        val w = size.width
        val h = size.height
        val pts = data.mapIndexed { i, m ->
            val x = if (data.size == 1) w / 2 else i * w / (data.size - 1)
            val y = h - ((m.value - min) / range) * h
            androidx.compose.ui.geometry.Offset(x, y)
        }

        val lineColor = android.graphics.Color.parseColor("#4ECDC4")
        val paint = androidx.compose.ui.graphics.Paint().apply {
            color = Color(lineColor)
            strokeWidth = 3f
            isAntiAlias = true
        }

        for (i in 0 until pts.size - 1) {
            drawLine(
                color = Color(0xFF4ECDC4),
                start = pts[i],
                end = pts[i + 1],
                strokeWidth = 2.5f
            )
        }
        pts.forEach { pt ->
            drawCircle(Color(0xFF4ECDC4), radius = 4f, center = pt)
        }
    }
    // Latest value
    data.lastOrNull()?.let {
        Text("Latest: ${it.value}", color = SubText, fontSize = 11.sp)
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

@Composable
private fun StatChip(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun EmptyState(msg: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(msg, color = SubText, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

private fun formatTimestamp(ts: String): String {
    return try {
        val instant = Instant.parse(if (ts.endsWith("Z")) ts else "${ts}Z")
        val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy · HH:mm")
            .withZone(ZoneId.systemDefault())
        formatter.format(instant)
    } catch (e: Exception) {
        ts
    }
}
