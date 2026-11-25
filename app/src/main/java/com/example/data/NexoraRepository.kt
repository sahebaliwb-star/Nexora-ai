package com.example.data

import android.content.Context
import com.example.data.local.ActivityLogEntity
import com.example.data.local.AiMemoryEntity
import com.example.data.local.DemoTradeEntity
import com.example.data.local.NexoraDatabase
import com.example.data.local.NexoraPreferences
import com.example.data.local.NexoraSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class BrokerHealthResult(
    val isReachable: Boolean,
    val statusCode: Int?,
    val message: String
)

class NexoraRepository(context: Context) {
    private val db = NexoraDatabase.getInstance(context)
    private val activityDao = db.activityLogDao()
    private val memoryDao = db.aiMemoryDao()
    private val tradeDao = db.demoTradeDao()
    val preferences = NexoraPreferences(context)

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .build()
    }

    val recentActivities: Flow<List<ActivityLogEntity>> = activityDao.observeRecentLogs()
    val memories: Flow<List<AiMemoryEntity>> = memoryDao.observeMemories()
    val demoTrades: Flow<List<DemoTradeEntity>> = tradeDao.observeAllTrades()
    val settings: Flow<NexoraSettings> = preferences.settingsFlow

    suspend fun logActivity(
        module: String,
        userCommand: String,
        aiResponse: String,
        status: String = "COMPLETED"
    ) {
        activityDao.insertLog(
            ActivityLogEntity(
                module = module,
                userCommand = userCommand,
                aiResponse = aiResponse,
                status = status
            )
        )
    }

    suspend fun clearAllActivities() = activityDao.clearAllLogs()

    suspend fun getMemoryContextStrings(): List<String> {
        return memoryDao.getSnapshotMemories().map { "${it.label}: ${it.detail} (${it.category})" }
    }

    suspend fun addMemory(label: String, detail: String, category: String) {
        if (label.isBlank() || detail.isBlank()) return
        memoryDao.upsertMemory(
            AiMemoryEntity(
                label = label.trim(),
                detail = detail.trim(),
                category = category
            )
        )
    }

    suspend fun deleteMemory(id: Long) = memoryDao.deleteMemoryById(id)

    suspend fun clearAllMemories() = memoryDao.clearAllMemories()

    suspend fun addDemoTrade(
        symbol: String,
        side: String,
        entryPrice: Double,
        targetPrice: Double,
        stopLoss: Double,
        quantity: Double,
        notes: String,
        isLiveConfirmed: Boolean = false
    ) {
        tradeDao.insertTrade(
            DemoTradeEntity(
                symbol = symbol.trim().uppercase(),
                side = side.uppercase(),
                entryPrice = entryPrice,
                targetPrice = targetPrice,
                stopLoss = stopLoss,
                quantity = quantity,
                status = "OPEN",
                notes = notes.trim(),
                isLiveConfirmed = isLiveConfirmed
            )
        )
    }

    suspend fun closeDemoTrade(trade: DemoTradeEntity, exitPrice: Double) {
        tradeDao.updateTrade(
            trade.copy(
                exitPrice = exitPrice,
                status = "CLOSED"
            )
        )
    }

    suspend fun deleteDemoTrade(id: Long) = tradeDao.deleteTradeById(id)

    suspend fun clearAllTrades() = tradeDao.clearAllTrades()

    suspend fun testBrokerEndpoint(endpointUrl: String): BrokerHealthResult =
        withContext(Dispatchers.IO) {
            val trimmed = endpointUrl.trim()
            if (!trimmed.startsWith("https://")) {
                return@withContext BrokerHealthResult(
                    isReachable = false,
                    statusCode = null,
                    message = "নিরাপত্তার জন্য শুধুমাত্র HTTPS Broker Endpoint URL ব্যবহার করুন।"
                )
            }
            try {
                val request = Request.Builder()
                    .url(trimmed)
                    .get()
                    .header("Accept", "application/json")
                    .build()
                httpClient.newCall(request).execute().use { response ->
                    val reachable = response.code in 200..499
                    BrokerHealthResult(
                        isReachable = reachable,
                        statusCode = response.code,
                        message = if (response.isSuccessful) {
                            "Broker API সংযুক্ত (HTTP ${response.code}) — Live অর্ডারের আগে Confirmation সক্রিয়।"
                        } else {
                            "Endpoint উত্তর দিয়েছে (HTTP ${response.code})। API হেডার বা টোকেন যাচাই করুন।"
                        }
                    )
                }
            } catch (e: Exception) {
                BrokerHealthResult(
                    isReachable = false,
                    statusCode = null,
                    message = "সংযোগ ব্যর্থ: ${e.localizedMessage ?: "Network error"}"
                )
            }
        }
}
