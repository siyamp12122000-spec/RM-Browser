package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.FilterRule
import com.example.data.model.SecurityEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface SecurityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: SecurityEvent): Long

    @Query("SELECT * FROM security_events ORDER BY timestamp DESC LIMIT 100")
    fun getRecentEvents(): Flow<List<SecurityEvent>>

    @Query("SELECT COUNT(*) FROM security_events WHERE actionTaken = 'BLOCKED'")
    fun getTotalThreatsBlocked(): Flow<Int>

    @Query("SELECT COUNT(*) FROM security_events WHERE threatType IN ('MALWARE', 'PHISHING', 'SCAM') AND actionTaken = 'BLOCKED'")
    fun getMalwarePhishingBlocked(): Flow<Int>

    @Query("SELECT COUNT(*) FROM security_events WHERE threatType = 'AD' AND actionTaken = 'BLOCKED'")
    fun getAdsBlocked(): Flow<Int>

    @Query("SELECT COUNT(*) FROM security_events WHERE threatType = 'TRACKER' AND actionTaken = 'BLOCKED'")
    fun getTrackersBlocked(): Flow<Int>

    @Query("SELECT COUNT(*) FROM security_events WHERE threatType IN ('ADULT', 'GAMBLING', 'DRUGS', 'VIOLENCE') AND actionTaken = 'BLOCKED'")
    fun getIslamicSafeBlocked(): Flow<Int>

    @Query("DELETE FROM security_events")
    suspend fun clearAllEvents()

    // Filter rules
    @Query("SELECT * FROM filter_rules ORDER BY createdAt DESC")
    fun getAllRules(): Flow<List<FilterRule>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: FilterRule): Long

    @Delete
    suspend fun deleteRule(rule: FilterRule)

    @Query("DELETE FROM filter_rules WHERE pattern = :pattern")
    suspend fun deleteRuleByPattern(pattern: String)

    @Query("SELECT * FROM filter_rules WHERE isEnabled = 1")
    suspend fun getActiveRules(): List<FilterRule>
}
