package com.streamrecorder.tv.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import androidx.lifecycle.*
import com.streamrecorder.core.model.Recording
import com.streamrecorder.core.model.Target
import com.streamrecorder.core.player.PlayerLauncher
import com.streamrecorder.tv.di.AppContainer
import kotlinx.coroutines.launch

class MainViewModel(private val container: AppContainer) : ViewModel() {

    sealed class UiState {
        data class Loading(val hasCachedData: Boolean) : UiState()
        data class Ready(val selectedTarget: Target?) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableLiveData<UiState>()
    val uiState: LiveData<UiState> = _uiState

    private val _targets = MutableLiveData<List<Target>>()
    val targets: LiveData<List<Target>> = _targets

    private val _recordings = MutableLiveData<List<Recording>?>()
    val recordings: LiveData<List<Recording>?> = _recordings

    private val _selectedTarget = MutableLiveData<Target?>()
    val selectedTarget: LiveData<Target?> = _selectedTarget

    private val _isRefreshing = MutableLiveData(false)
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    private val _toastMessage = MutableLiveData<String?>()
    val toastMessage: LiveData<String?> = _toastMessage

    fun initialize() {
        val cached = container.repository.getCachedTargets()
        if (cached != null) {
            _targets.value = sortTargets(cached.first)
            _uiState.value = UiState.Loading(hasCachedData = true)
        } else {
            _uiState.value = UiState.Loading(hasCachedData = false)
        }

        viewModelScope.launch {
            try {
                container.repository.detectServer()
                val (freshTargets, _) = container.repository.getTargets(forceRefresh = true)
                _targets.value = sortTargets(freshTargets)
                _uiState.value = UiState.Ready(selectedTarget = _selectedTarget.value)

                // Background sync watch positions
                container.repository.syncWatchPositions()
            } catch (e: Exception) {
                if (cached != null) {
                    _uiState.value = UiState.Ready(selectedTarget = _selectedTarget.value)
                    _toastMessage.value = "Offline — showing cached data"
                } else {
                    _uiState.value = UiState.Error("Can't connect to server")
                }
            }
        }
    }

    fun selectStreamer(target: Target) {
        _selectedTarget.value = target
        _recordings.value = null
        _uiState.value = UiState.Ready(selectedTarget = target)

        val cached = container.repository.getCachedRecordings(target.id)
        if (cached != null) {
            _recordings.value = cached.data.filter { !it.isRunning }
        }

        viewModelScope.launch {
            try {
                val data = container.repository.getRecordings(target.id, forceRefresh = cached != null)
                _recordings.value = data.data.filter { !it.isRunning }
            } catch (e: Exception) {
                if (cached == null) {
                    _toastMessage.value = "Couldn't load recordings"
                }
            }
        }
    }

    fun deselectStreamer() {
        _selectedTarget.value = null
        _recordings.value = null
        _uiState.value = UiState.Ready(selectedTarget = null)
    }

    fun refresh() {
        _isRefreshing.value = true
        viewModelScope.launch {
            try {
                val (freshTargets, _) = container.repository.fullRefresh()
                _targets.value = sortTargets(freshTargets)

                val selected = _selectedTarget.value
                if (selected != null) {
                    val data = container.repository.getRecordings(selected.id, forceRefresh = true)
                    _recordings.value = data.data.filter { !it.isRunning }
                }
                _toastMessage.value = "Refresh complete"
            } catch (e: Exception) {
                _toastMessage.value = "Refresh failed"
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun playRecording(activity: Activity, recording: Recording) {
        val source = recording.bestSource ?: return
        val baseUrl = container.repository.getBaseUrl() ?: return

        val player = when (container.cache.defaultPlayer) {
            "mx" -> PlayerLauncher.Player.MX
            "generic" -> PlayerLauncher.Player.GENERIC
            else -> PlayerLauncher.Player.MPV
        }

        val intent = container.playerLauncher.playRecording(
            recording = recording,
            source = source,
            player = player,
            baseUrl = baseUrl,
            useMxPro = container.cache.mxUsePro,
        )

        try {
            activity.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // Fallback to generic player
            try {
                val fallback = container.playerLauncher.playRecording(
                    recording, source, PlayerLauncher.Player.GENERIC, baseUrl, useStableUrl = false,
                )
                activity.startActivity(fallback)
            } catch (_: ActivityNotFoundException) {
                _toastMessage.value = "No video player found"
            }
        }
    }

    fun onRecordingLongPress(recording: Recording) {
        // TODO: show context menu dialog
    }

    private fun sortTargets(targets: List<Target>): List<Target> {
        return targets.sortedWith(compareByDescending<Target> { it.isLive }.thenByDescending { it.latestTs })
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(container) as T
        }
    }
}
