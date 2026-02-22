package com.gymtracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gymtracker.data.SettingsRepository
import com.gymtracker.data.api.RetrofitClient
import com.gymtracker.data.models.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class UiEvent {
    data class Success(val message: String) : UiEvent()
    data class Error(val message: String) : UiEvent()
}

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val settings = SettingsRepository(app)

    // ── Settings ──────────────────────────────────────────────────────────────
    val baseUrl: StateFlow<String> = settings.baseUrl
        .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsRepository.DEFAULT_URL)

    fun saveBaseUrl(url: String) = viewModelScope.launch {
        settings.saveBaseUrl(url)
    }

    // ── UI Events (one-shot snackbar/toast) ───────────────────────────────────
    private val _events = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _events

    private suspend fun emit(event: UiEvent) = _events.emit(event)

    // ── Exercises ─────────────────────────────────────────────────────────────
    private val _exercises = MutableStateFlow<List<Exercise>>(emptyList())
    val exercises: StateFlow<List<Exercise>> = _exercises

    fun loadExercises() = viewModelScope.launch {
        runCatching { RetrofitClient.service.getExercises() }
            .onSuccess { resp -> if (resp.isSuccessful) _exercises.value = resp.body() ?: emptyList() }
            .onFailure { emit(UiEvent.Error("Failed to load exercises: ${it.message}")) }
    }

    fun addExercise(name: String, category: String) = viewModelScope.launch {
        runCatching { RetrofitClient.service.createExercise(ExerciseCreate(name, category)) }
            .onSuccess { resp ->
                if (resp.isSuccessful) {
                    emit(UiEvent.Success("Exercise added"))
                    loadExercises()
                } else emit(UiEvent.Error("Failed: ${resp.code()}"))
            }
            .onFailure { emit(UiEvent.Error(it.message ?: "Network error")) }
    }

    fun deleteExercise(id: Int) = viewModelScope.launch {
        runCatching { RetrofitClient.service.deleteExercise(id) }
            .onSuccess { emit(UiEvent.Success("Exercise deleted")); loadExercises() }
            .onFailure { emit(UiEvent.Error(it.message ?: "Network error")) }
    }

    fun updateExercise(id: Int, name: String, category: String) = viewModelScope.launch {
        runCatching { RetrofitClient.service.updateExercise(id, ExerciseCreate(name, category)) }
            .onSuccess { resp ->
                if (resp.isSuccessful) { emit(UiEvent.Success("Updated")); loadExercises() }
                else emit(UiEvent.Error("Failed: ${resp.code()}"))
            }
            .onFailure { emit(UiEvent.Error(it.message ?: "Network error")) }
    }

    // ── Measurement Types ─────────────────────────────────────────────────────
    private val _measurementTypes = MutableStateFlow<List<MeasurementType>>(emptyList())
    val measurementTypes: StateFlow<List<MeasurementType>> = _measurementTypes

    fun loadMeasurementTypes() = viewModelScope.launch {
        runCatching { RetrofitClient.service.getMeasurementTypes() }
            .onSuccess { resp -> if (resp.isSuccessful) _measurementTypes.value = resp.body() ?: emptyList() }
            .onFailure { emit(UiEvent.Error("Failed to load measurement types")) }
    }

    fun addMeasurementType(name: String) = viewModelScope.launch {
        runCatching { RetrofitClient.service.createMeasurementType(mapOf("name" to name)) }
            .onSuccess { resp ->
                if (resp.isSuccessful) { emit(UiEvent.Success("Added")); loadMeasurementTypes() }
                else emit(UiEvent.Error("Failed"))
            }
            .onFailure { emit(UiEvent.Error(it.message ?: "Network error")) }
    }

    fun deleteMeasurementType(id: Int) = viewModelScope.launch {
        runCatching { RetrofitClient.service.deleteMeasurementType(id) }
            .onSuccess { emit(UiEvent.Success("Deleted")); loadMeasurementTypes() }
            .onFailure { emit(UiEvent.Error(it.message ?: "Network error")) }
    }

    // ── Workouts ──────────────────────────────────────────────────────────────
    private val _workouts = MutableStateFlow<List<Workout>>(emptyList())
    val workouts: StateFlow<List<Workout>> = _workouts

    fun loadWorkouts() = viewModelScope.launch {
        runCatching { RetrofitClient.service.getWorkouts() }
            .onSuccess { resp -> if (resp.isSuccessful) _workouts.value = resp.body() ?: emptyList() }
            .onFailure { emit(UiEvent.Error("Failed to load workouts")) }
    }

    fun logWorkout(exerciseName: String, sets: Int, reps: Int, weight: Float, tempo: String) = viewModelScope.launch {
        runCatching {
            RetrofitClient.service.logWorkout(WorkoutCreate(exerciseName, sets, reps, weight, tempo))
        }.onSuccess { resp ->
            if (resp.isSuccessful) {
                emit(UiEvent.Success("✅ Workout logged to Pi!"))
                loadWorkouts()
            } else emit(UiEvent.Error("Server error: ${resp.code()}"))
        }.onFailure { emit(UiEvent.Error("Network error: ${it.message}")) }
    }

    fun deleteWorkout(id: Int) = viewModelScope.launch {
        runCatching { RetrofitClient.service.deleteWorkout(id) }
            .onSuccess { emit(UiEvent.Success("Deleted")); loadWorkouts() }
            .onFailure { emit(UiEvent.Error(it.message ?: "Network error")) }
    }

    // ── Metrics ───────────────────────────────────────────────────────────────
    private val _metrics = MutableStateFlow<List<UserMetric>>(emptyList())
    val metrics: StateFlow<List<UserMetric>> = _metrics

    fun loadMetrics() = viewModelScope.launch {
        runCatching { RetrofitClient.service.getMetrics() }
            .onSuccess { resp -> if (resp.isSuccessful) _metrics.value = resp.body() ?: emptyList() }
            .onFailure { emit(UiEvent.Error("Failed to load metrics")) }
    }

    fun logMetric(metricType: String, value: Float, notes: String) = viewModelScope.launch {
        runCatching {
            RetrofitClient.service.logMetric(MetricCreate(metricType, value, notes))
        }.onSuccess { resp ->
            if (resp.isSuccessful) {
                emit(UiEvent.Success("✅ Metrics synced to Pi!"))
                loadMetrics()
            } else emit(UiEvent.Error("Server error: ${resp.code()}"))
        }.onFailure { emit(UiEvent.Error("Network error: ${it.message}")) }
    }

    fun deleteMetric(id: Int) = viewModelScope.launch {
        runCatching { RetrofitClient.service.deleteMetric(id) }
            .onSuccess { emit(UiEvent.Success("Deleted")); loadMetrics() }
            .onFailure { emit(UiEvent.Error(it.message ?: "Network error")) }
    }

    // ── Init ──────────────────────────────────────────────────────────────────
    init {
        viewModelScope.launch {
            settings.baseUrl.collect { url ->
                RetrofitClient.setBaseUrl(url)
            }
        }
        loadExercises()
        loadMeasurementTypes()
        loadWorkouts()
        loadMetrics()
    }
}
