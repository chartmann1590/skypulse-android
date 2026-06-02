package com.charles.skypulse.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.charles.skypulse.app.data.local.entity.AlertRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: AlertRuleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rules: List<AlertRuleEntity>)

    @Query("SELECT * FROM alert_rules")
    fun observeAll(): Flow<List<AlertRuleEntity>>

    @Query("SELECT * FROM alert_rules")
    suspend fun getAll(): List<AlertRuleEntity>

    @Query("SELECT * FROM alert_rules WHERE enabled = 1")
    suspend fun getEnabled(): List<AlertRuleEntity>

    @Query("SELECT COUNT(*) FROM alert_rules WHERE enabled = 1")
    suspend fun enabledCount(): Int
}
