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

    // ── Profiles ──────────────────────────────────────────────────────────────
    private val _profiles = MutableStateFlow<List<Profile>>(emptyList())
    val profiles: StateFlow<List<Profile>> = _profiles

    private val _currentProfile = MutableStateFlow<Profile?>(null)
    val currentProfile: StateFlow<Profile?> = _currentProfile

    private val _isLoadingProfile = MutableStateFlow(false)
    val isLoadingProfile: StateFlow<Boolean> = _isLoadingProfile

    fun loadProfiles() = viewModelScope.launch {
        runCatching { RetrofitClient.service.getProfiles() }
            .onSuccess { resp -> if (resp.isSuccessful) _profiles.value = resp.body() ?: emptyList() }
            .onFailure { /* silently ignore — shown via login screen state */ }
    }

    fun selectProfile(id: Int) = viewModelScope.launch {
        _isLoadingProfile.value = true
        runCatching { RetrofitClient.service.getProfile(id) }
            .onSuccess { resp ->
                if (resp.isSuccessful) {
                    val profile = resp.body()
                    _currentProfile.value = profile
                    settings.saveProfileId(profile?.id)
                    loadWorkouts()
                    loadMetrics()
                } else {
                    emit(UiEvent.Error("Failed to load profile: ${resp.code()}"))
                }
            }
            .onFailure { emit(UiEvent.Error("Network error: ${it.message}")) }
        _isLoadingProfile.value = false
    }

    fun signOut() = viewModelScope.launch {
        settings.saveProfileId(null)
        _currentProfile.value = null
        _workouts.value = emptyList()
        _metrics.value = emptyList()
    }

    fun createProfile(name: String) = viewModelScope.launch {
        runCatching { RetrofitClient.service.createProfile(ProfileCreate(name)) }
            .onSuccess { resp ->
                if (resp.isSuccessful) {
                    val profile = resp.body()
                    if (profile != null) {
                        loadProfiles()
                        selectProfile(profile.id)
                    }
                } else {
                    emit(UiEvent.Error("Failed to create profile: ${resp.code()}"))
                }
            }
            .onFailure { emit(UiEvent.Error("Network error: ${it.message}")) }
    }

    fun updateCurrentProfile(update: ProfileUpdate) = viewModelScope.launch {
        val profileId = _currentProfile.value?.id ?: return@launch
        runCatching { RetrofitClient.service.updateProfile(profileId, update) }
            .onSuccess { resp ->
                if (resp.isSuccessful) {
                    _currentProfile.value = resp.body()
                    emit(UiEvent.Success("Profile updated"))
                } else {
                    emit(UiEvent.Error("Failed to update: ${resp.code()}"))
                }
            }
            .onFailure { emit(UiEvent.Error("Network error: ${it.message}")) }
    }

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
        val profileId = _currentProfile.value?.id
        runCatching { RetrofitClient.service.getWorkouts(profileId) }
            .onSuccess { resp -> if (resp.isSuccessful) _workouts.value = resp.body() ?: emptyList() }
            .onFailure { emit(UiEvent.Error("Failed to load workouts")) }
    }

    fun logWorkout(exerciseName: String, sets: Int, reps: Int, weight: Float, tempo: String) = viewModelScope.launch {
        val profileId = _currentProfile.value?.id
        runCatching {
            RetrofitClient.service.logWorkout(WorkoutCreate(exerciseName, sets, reps, weight, tempo, profileId))
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
        val profileId = _currentProfile.value?.id
        runCatching { RetrofitClient.service.getMetrics(profileId) }
            .onSuccess { resp -> if (resp.isSuccessful) _metrics.value = resp.body() ?: emptyList() }
            .onFailure { emit(UiEvent.Error("Failed to load metrics")) }
    }

    fun logMetric(metricType: String, value: Float, notes: String) = viewModelScope.launch {
        val profileId = _currentProfile.value?.id
        runCatching {
            RetrofitClient.service.logMetric(MetricCreate(metricType, value, notes, profileId))
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
        // Keep RetrofitClient base URL in sync with DataStore
        viewModelScope.launch {
            settings.baseUrl.collect { url ->
                RetrofitClient.setBaseUrl(url)
            }
        }
        // Restore saved profile on startup
        viewModelScope.launch {
            settings.profileIdFlow.collect { savedId ->
                if (savedId != null && _currentProfile.value == null) {
                    _isLoadingProfile.value = true
                    runCatching { RetrofitClient.service.getProfile(savedId) }
                        .onSuccess { resp ->
                            if (resp.isSuccessful) {
                                _currentProfile.value = resp.body()
                                loadWorkouts()
                                loadMetrics()
                            } else {
                                // Saved profile no longer exists — clear it
                                settings.saveProfileId(null)
                            }
                        }
                        .onFailure { /* server unreachable — stay on login screen */ }
                    _isLoadingProfile.value = false
                }
            }
        }
        loadExercises()
        loadMeasurementTypes()
        loadProfiles()
    }
}
