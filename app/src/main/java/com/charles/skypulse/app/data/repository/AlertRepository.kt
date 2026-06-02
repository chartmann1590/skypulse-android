package com.charles.skypulse.app.data.repository

import com.charles.skypulse.app.data.local.dao.AlertDao
import com.charles.skypulse.app.data.local.entity.AlertRuleEntity
import com.charles.skypulse.app.domain.model.AlertRule
import com.charles.skypulse.app.domain.model.AlertType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlertRepository @Inject constructor(
    private val alertDao: AlertDao,
) {
    val rules: Flow<List<AlertRule>> = alertDao.observeAll().map { list ->
        list.mapNotNull { it.toDomain() }
    }

    suspend fun getAll(): List<AlertRule> = alertDao.getAll().mapNotNull { it.toDomain() }

    suspend fun getEnabled(): List<AlertRule> = alertDao.getEnabled().mapNotNull { it.toDomain() }

    suspend fun enabledCount(): Int = alertDao.enabledCount()

    suspend fun saveAll(rules: List<AlertRule>) =
        alertDao.upsertAll(rules.map { it.toEntity() })

    suspend fun save(rule: AlertRule) = alertDao.upsert(rule.toEntity())
}

private fun AlertRule.toEntity() = AlertRuleEntity(
    type = type.name,
    enabled = enabled,
    radiusNm = radiusNm,
    callsign = callsign,
    altitudeThresholdFeet = altitudeThresholdFeet,
    anchorLat = anchorLat,
    anchorLon = anchorLon,
)

private fun AlertRuleEntity.toDomain(): AlertRule? {
    val parsedType = runCatching { AlertType.valueOf(type) }.getOrNull() ?: return null
    return AlertRule(
        type = parsedType,
        enabled = enabled,
        radiusNm = radiusNm,
        callsign = callsign,
        altitudeThresholdFeet = altitudeThresholdFeet,
        anchorLat = anchorLat,
        anchorLon = anchorLon,
    )
}
