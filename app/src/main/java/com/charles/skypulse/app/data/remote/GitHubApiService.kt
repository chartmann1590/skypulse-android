package com.charles.skypulse.app.data.remote

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Talks to the cloudflare-worker/ feedback relay, not api.github.com directly. See
 * NetworkModule.provideGitHubRetrofit and cloudflare-worker/src/index.ts.
 */
interface GitHubApiService {

    @POST("issue")
    suspend fun createIssue(@Body request: CreateIssueRequest): GitHubIssueDto

    @GET("issue/{number}")
    suspend fun getIssue(@Path("number") number: Int): GitHubIssueDto

    @GET("issue/{number}/comments")
    suspend fun getComments(@Path("number") number: Int): List<GitHubCommentDto>

    @POST("issue/{number}/comments")
    suspend fun postComment(
        @Path("number") number: Int,
        @Body request: PostCommentRequest
    ): GitHubCommentDto

    @POST("upload-image")
    suspend fun uploadAsset(@Body request: UploadAssetRequest): UploadAssetResponse
}

@Serializable
data class CreateIssueRequest(
    val title: String,
    val body: String
)

@Serializable
data class PostCommentRequest(
    val body: String
)

@Serializable
data class UploadAssetRequest(
    val filename: String,
    val contentBase64: String // Base64-encoded file content
)

@Serializable
data class GitHubIssueDto(
    val number: Int,
    val title: String,
    val state: String,
    val created_at: String,
    val html_url: String,
    val body: String? = null
)

@Serializable
data class GitHubCommentDto(
    val id: Long,
    val body: String,
    val created_at: String,
    val user: GitHubUserDto
)

@Serializable
data class GitHubUserDto(
    val login: String
)

@Serializable
data class UploadAssetResponse(
    val content: UploadAssetContentDto
)

@Serializable
data class UploadAssetContentDto(
    val download_url: String
)
