package com.charles.skypulse.app.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.charles.skypulse.app.data.local.dao.CacheDao
import com.charles.skypulse.app.data.repository.FeedbackRepository
import com.charles.skypulse.app.data.settings.SettingsDataStore
import com.charles.skypulse.app.data.settings.SkySettings
import com.charles.skypulse.app.data.remote.GitHubCommentDto
import com.charles.skypulse.app.domain.model.AltitudeUnit
import com.charles.skypulse.app.domain.model.DistanceUnit
import com.charles.skypulse.app.domain.model.SavedBugReport
import com.charles.skypulse.app.domain.model.SpeedUnit
import com.charles.skypulse.app.domain.util.DiagnosticsGatherer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val cacheDao: CacheDao,
    private val feedbackRepository: FeedbackRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    val settings: StateFlow<SkySettings> = settingsDataStore.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SkySettings())

    private val _cacheCleared = MutableStateFlow(false)
    val cacheCleared: StateFlow<Boolean> = _cacheCleared.asStateFlow()

    // ---- Feedback System State ----
    val submittedIssues: StateFlow<List<SavedBugReport>> = feedbackRepository.submittedIssues
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _comments = MutableStateFlow<List<GitHubCommentDto>>(emptyList())
    val comments: StateFlow<List<GitHubCommentDto>> = _comments.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        refreshIssues()
    }

    fun refreshIssues() {
        viewModelScope.launch {
            feedbackRepository.refreshIssueStatuses()
        }
    }

    fun createIssue(
        title: String,
        description: String,
        screenshotBytes: ByteArray?,
        screenshotFileName: String?,
        includeDiagnostics: Boolean,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            _errorMessage.value = null
            try {
                val diagnostics = if (includeDiagnostics) {
                    DiagnosticsGatherer(context).gatherDiagnostics()
                } else {
                    null
                }
                feedbackRepository.submitIssue(
                    title = title,
                    description = description,
                    screenshotBytes = screenshotBytes,
                    screenshotFileName = screenshotFileName,
                    systemDiagnostics = diagnostics
                )
                onSuccess()
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to submit issue"
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    fun loadComments(issueNumber: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val list = feedbackRepository.getComments(issueNumber)
                _comments.value = list
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to load comments"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun postComment(
        issueNumber: Int,
        body: String,
        screenshotBytes: ByteArray?,
        screenshotFileName: String?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                feedbackRepository.postComment(
                    number = issueNumber,
                    body = body,
                    screenshotBytes = screenshotBytes,
                    screenshotFileName = screenshotFileName
                )
                val list = feedbackRepository.getComments(issueNumber)
                _comments.value = list
                onSuccess()
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to post comment"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun setRefreshInterval(seconds: Int) =
        viewModelScope.launch { settingsDataStore.setRefreshInterval(seconds) }

    fun setAltitudeUnit(unit: AltitudeUnit) =
        viewModelScope.launch { settingsDataStore.setAltitudeUnit(unit) }

    fun setSpeedUnit(unit: SpeedUnit) =
        viewModelScope.launch { settingsDataStore.setSpeedUnit(unit) }

    fun setDistanceUnit(unit: DistanceUnit) =
        viewModelScope.launch { settingsDataStore.setDistanceUnit(unit) }

    fun clearCache() = viewModelScope.launch {
        cacheDao.clear()
        _cacheCleared.value = true
    }
}
