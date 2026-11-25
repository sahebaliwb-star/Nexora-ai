package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activity_logs")
data class ActivityLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val module: String, // ASSISTANT, APP_CONTROL, SCREEN, TRADING, GAMING
    val userCommand: String,
    val aiResponse: String,
    val status: String = "COMPLETED", // COMPLETED, REQUIRES_PERMISSION, ERROR
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "ai_memories")
data class AiMemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val detail: String,
    val category: String = "PREFERENCE", // PREFERENCE, TRADING, GAMING, WORKFLOW
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "demo_trades")
data class DemoTradeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val symbol: String,
    val side: String, // BUY or SELL
    val entryPrice: Double,
    val targetPrice: Double,
    val stopLoss: Double,
    val quantity: Double,
    val exitPrice: Double? = null,
    val status: String = "OPEN", // OPEN or CLOSED
    val notes: String = "",
    val isLiveConfirmed: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun computePnl(markPrice: Double = exitPrice ?: entryPrice): Double {
        val diff = if (side.equals("BUY", ignoreCase = true)) {
            markPrice - entryPrice
        } else {
            entryPrice - markPrice
        }
        return diff * quantity
    }
}
