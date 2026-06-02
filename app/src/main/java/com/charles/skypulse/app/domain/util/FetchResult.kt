package com.charles.skypulse.app.domain.util

import com.charles.skypulse.app.domain.model.Aircraft
import com.charles.skypulse.app.domain.model.DataSource

/** Outcome of a remote aircraft fetch attempt from a single provider. */
sealed interface FetchResult {
    data class Success(
        val aircraft: List<Aircraft>,
        val source: DataSource,
    ) : FetchResult

    /** Reached the provider but it returned no aircraft for the area. */
    data object Empty : FetchResult

    data class Error(val message: String, val cause: Throwable? = null) : FetchResult
}
