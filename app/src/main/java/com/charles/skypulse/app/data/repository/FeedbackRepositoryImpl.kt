package com.charles.skypulse.app.data.repository

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.charles.skypulse.app.BuildConfig
import com.charles.skypulse.app.data.remote.CreateIssueRequest
import com.charles.skypulse.app.data.remote.GitHubApiService
import com.charles.skypulse.app.data.remote.GitHubCommentDto
import com.charles.skypulse.app.data.remote.PostCommentRequest
import com.charles.skypulse.app.data.remote.UploadAssetRequest
import com.charles.skypulse.app.domain.model.SavedBugReport
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

val Context.feedbackDataStore: DataStore<Preferences> by preferencesDataStore(name = "feedback_reports")

@Singleton
class FeedbackRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: GitHubApiService,
    private val json: Json
) : FeedbackRepository {

    private object Keys {
        val SUBMITTED_ISSUES = stringPreferencesKey("submitted_issues")
    }

    override val submittedIssues: Flow<List<SavedBugReport>> = context.feedbackDataStore.data.map { p ->
        val jsonStr = p[Keys.SUBMITTED_ISSUES] ?: "[]"
        runCatching { json.decodeFromString<List<SavedBugReport>>(jsonStr) }.getOrDefault(emptyList())
    }

    override suspend fun submitIssue(
        title: String,
        description: String,
        screenshotBytes: ByteArray?,
        screenshotFileName: String?,
        systemDiagnostics: String?
    ): SavedBugReport {
        var issueBody = description

        // 1. Upload screenshot if available
        if (screenshotBytes != null) {
            val fileName = screenshotFileName ?: "screenshot_${System.currentTimeMillis()}.png"
            val base64Content = Base64.encodeToString(screenshotBytes, Base64.NO_WRAP)
            try {
                val uploadResponse = apiService.uploadAsset(
                    owner = BuildConfig.GITHUB_REPO_OWNER,
                    repo = BuildConfig.GITHUB_REPO_NAME,
                    filename = fileName,
                    request = UploadAssetRequest(
                        message = "Upload feedback screenshot: $fileName",
                        content = base64Content
                    )
                )
                val imageUrl = uploadResponse.content.download_url
                issueBody += "\n\n![Screenshot]($imageUrl)"
            } catch (e: Exception) {
                issueBody += "\n\n_(Failed to upload screenshot: ${e.localizedMessage})_"
            }
        }

        // 2. Append system diagnostics if available
        if (systemDiagnostics != null) {
            issueBody += "\n\n---\n$systemDiagnostics"
        }

        // 3. Create issue on GitHub
        val issueDto = apiService.createIssue(
            owner = BuildConfig.GITHUB_REPO_OWNER,
            repo = BuildConfig.GITHUB_REPO_NAME,
            request = CreateIssueRequest(title = title, body = issueBody)
        )

        // 4. Save issue locally
        val newReport = SavedBugReport(
            number = issueDto.number,
            title = issueDto.title,
            status = issueDto.state,
            createdAt = issueDto.created_at,
            htmlUrl = issueDto.html_url
        )

        val currentList = submittedIssues.first().toMutableList()
        currentList.add(newReport)
        saveLocalIssues(currentList)

        return newReport
    }

    override suspend fun getComments(number: Int): List<GitHubCommentDto> {
        return apiService.getComments(
            owner = BuildConfig.GITHUB_REPO_OWNER,
            repo = BuildConfig.GITHUB_REPO_NAME,
            number = number
        )
    }

    override suspend fun postComment(
        number: Int,
        body: String,
        screenshotBytes: ByteArray?,
        screenshotFileName: String?
    ): GitHubCommentDto {
        // App replies prepend a clear marker to distinguish replies
        var commentBody = "**[User Reply from App]**\n\n$body"

        // Upload screenshot if available
        if (screenshotBytes != null) {
            val fileName = screenshotFileName ?: "reply_screenshot_${System.currentTimeMillis()}.png"
            val base64Content = Base64.encodeToString(screenshotBytes, Base64.NO_WRAP)
            try {
                val uploadResponse = apiService.uploadAsset(
                    owner = BuildConfig.GITHUB_REPO_OWNER,
                    repo = BuildConfig.GITHUB_REPO_NAME,
                    filename = fileName,
                    request = UploadAssetRequest(
                        message = "Upload comment screenshot: $fileName",
                        content = base64Content
                    )
                )
                val imageUrl = uploadResponse.content.download_url
                commentBody += "\n\n![Screenshot]($imageUrl)"
            } catch (e: Exception) {
                commentBody += "\n\n_(Failed to upload screenshot: ${e.localizedMessage})_"
            }
        }

        return apiService.postComment(
            owner = BuildConfig.GITHUB_REPO_OWNER,
            repo = BuildConfig.GITHUB_REPO_NAME,
            number = number,
            request = PostCommentRequest(body = commentBody)
        )
    }

    override suspend fun refreshIssueStatuses() {
        val currentList = submittedIssues.first()
        if (currentList.isEmpty()) return

        val updatedList = currentList.map { report ->
            try {
                val liveIssue = apiService.getIssue(
                    owner = BuildConfig.GITHUB_REPO_OWNER,
                    repo = BuildConfig.GITHUB_REPO_NAME,
                    number = report.number
                )
                report.copy(status = liveIssue.state)
            } catch (e: Exception) {
                report
            }
        }
        saveLocalIssues(updatedList)
    }

    private suspend fun saveLocalIssues(list: List<SavedBugReport>) {
        context.feedbackDataStore.edit { p ->
            p[Keys.SUBMITTED_ISSUES] = json.encodeToString(list)
        }
    }
}
