package com.charles.skypulse.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SavedBugReport(
    val number: Int,
    val title: String,
    val status: String,
    val createdAt: String,
    val htmlUrl: String
)
