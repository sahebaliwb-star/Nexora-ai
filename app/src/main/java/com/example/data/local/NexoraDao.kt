package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityLogDao {
    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC LIMIT 50")
    fun observeRecentLogs(): Flow<List<ActivityLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ActivityLogEntity): Long

    @Query("DELETE FROM activity_logs")
    suspend fun clearAllLogs()
}

@Dao
interface AiMemoryDao {
    @Query("SELECT * FROM ai_memories ORDER BY updatedAt DESC")
    fun observeMemories(): Flow<List<AiMemoryEntity>>

    @Query("SELECT * FROM ai_memories ORDER BY updatedAt DESC LIMIT 15")
    suspend fun getSnapshotMemories(): List<AiMemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMemory(memory: AiMemoryEntity): Long

    @Query("DELETE FROM ai_memories WHERE id = :id")
    suspend fun deleteMemoryById(id: Long)

    @Query("DELETE FROM ai_memories")
    suspend fun clearAllMemories()
}

@Dao
interface DemoTradeDao {
    @Query("SELECT * FROM demo_trades ORDER BY timestamp DESC")
    fun observeAllTrades(): Flow<List<DemoTradeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrade(trade: DemoTradeEntity): Long

    @Update
    suspend fun updateTrade(trade: DemoTradeEntity)

    @Query("DELETE FROM demo_trades WHERE id = :id")
    suspend fun deleteTradeById(id: Long)

    @Query("DELETE FROM demo_trades")
    suspend fun clearAllTrades()
}
