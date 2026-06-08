package com.charles.skypulse.app.data.remote

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface GitHubApiService {

    @POST("repos/{owner}/{repo}/issues")
    suspend fun createIssue(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body request: CreateIssueRequest
    ): GitHubIssueDto

    @GET("repos/{owner}/{repo}/issues/{number}")
    suspend fun getIssue(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("number") number: Int
    ): GitHubIssueDto

    @GET("repos/{owner}/{repo}/issues/{number}/comments")
    suspend fun getComments(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("number") number: Int
    ): List<GitHubCommentDto>

    @POST("repos/{owner}/{repo}/issues/{number}/comments")
    suspend fun postComment(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("number") number: Int,
        @Body request: PostCommentRequest
    ): GitHubCommentDto

    @PUT("repos/{owner}/{repo}/contents/feedback-assets/{filename}")
    suspend fun uploadAsset(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("filename") filename: String,
        @Body request: UploadAssetRequest
    ): UploadAssetResponse
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
    val message: String,
    val content: String // Base64-encoded file content
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
