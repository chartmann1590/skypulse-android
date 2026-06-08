package com.charles.skypulse.app.data.repository

import com.charles.skypulse.app.data.remote.GitHubCommentDto
import com.charles.skypulse.app.domain.model.SavedBugReport
import kotlinx.coroutines.flow.Flow

interface FeedbackRepository {
    val submittedIssues: Flow<List<SavedBugReport>>

    suspend fun submitIssue(
        title: String,
        description: String,
        screenshotBytes: ByteArray?,
        screenshotFileName: String?,
        systemDiagnostics: String?
    ): SavedBugReport

    suspend fun getComments(number: Int): List<GitHubCommentDto>

    suspend fun postComment(
        number: Int,
        body: String,
        screenshotBytes: ByteArray?,
        screenshotFileName: String?
    ): GitHubCommentDto

    suspend fun refreshIssueStatuses()
}
