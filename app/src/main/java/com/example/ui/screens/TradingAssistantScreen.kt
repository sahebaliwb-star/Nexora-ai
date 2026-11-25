package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Api
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.ScreenshotMonitor
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ai.TradingAnalysisResult
import com.example.data.BrokerHealthResult
import com.example.data.local.DemoTradeEntity
import com.example.data.local.NexoraSettings
import com.example.ui.VisionUiState
import com.example.ui.components.NexoraGlassCard
import com.example.ui.components.NexoraSectionHeader
import com.example.ui.components.NexoraStatusPill
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonBearish
import com.example.ui.theme.DeepNavyBackground
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldBullish
import com.example.ui.theme.MutedSlateText
import com.example.ui.theme.NavyBorder
import com.example.ui.theme.NavySurface
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SoftSlateText
import com.example.ui.theme.VioletNeon
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TradingAssistantScreen(
    settings: NexoraSettings,
    chartBitmap: Bitmap?,
    tradingVisionState: VisionUiState<TradingAnalysisResult>,
    demoTrades: List<DemoTradeEntity>,
    brokerHealth: BrokerHealthResult?,
    isTestingBroker: Boolean,
    onPickChartImage: () -> Unit,
    onCaptureChartScreen: () -> Unit,
    onAnalyzeChart: (symbol: String, timeframe: String, notes: String) -> Unit,
    onAddDemoTrade: (symbol: String, side: String, entry: Double, target: Double, sl: Double, qty: Double, notes: String, isLiveConfirmed: Boolean) -> Unit,
    onCloseDemoTrade: (DemoTradeEntity, Double) -> Unit,
    onDeleteDemoTrade: (Long) -> Unit,
    onSaveAndTestBroker: (name: String, url: String) -> Unit,
    onSpeakText: (String) -> Unit
) {
    var symbolInput by rememberSaveable { mutableStateOf("XAUUSD") }
    var selectedTimeframe by rememberSaveable { mutableStateOf("15m") }
    var extraContext by rememberSaveable { mutableStateOf("") }

    var showNewTradeDialog by remember { mutableStateOf(false) }
    var showLiveConfirmDialog by remember { mutableStateOf(false) }
    var tradeToClose by remember { mutableStateOf<DemoTradeEntity?>(null) }

    var brokerName by rememberSaveable(settings.brokerName) { mutableStateOf(settings.brokerName) }
    var brokerUrl by rememberSaveable(settings.brokerEndpointUrl) { mutableStateOf(settings.brokerEndpointUrl) }

    val startingBalance = 10_000.0
    val closedTrades = demoTrades.filter { it.status == "CLOSED" }
    val openTrades = demoTrades.filter { it.status == "OPEN" }
    val realizedPnl = closedTrades.sumOf { it.computePnl() }
    val winCount = closedTrades.count { it.computePnl() > 0.0 }
    val winRate = if (closedTrades.isNotEmpty()) (winCount * 100) / closedTrades.size else 0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 680.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Mandatory Risk Disclaimer Banner
        item {
            NexoraGlassCard(accentColor = AmberWarning) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = "Risk warning",
                        tint = AmberWarning,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(id = R.string.trading_risk_disclaimer),
                        style = MaterialTheme.typography.bodySmall,
                        color = PureWhite
                    )
                }
            }
        }

        // Profit / Loss & Demo Portfolio Dashboard
        item {
            NexoraSectionHeader(
                title = "Demo Trading P/L Dashboard",
                subtitle = "ভার্চুয়াল ব্যালেন্স ও ট্রেডিং পারফরম্যান্স",
                icon = Icons.Default.AccountBalanceWallet,
                accentColor = EmeraldBullish,
                trailing = {
                    Button(
                        onClick = { showNewTradeDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = DeepNavyBackground),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("add_demo_trade_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Demo Trade", style = MaterialTheme.typography.labelLarge)
                    }
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            NexoraGlassCard(accentColor = if (realizedPnl >= 0) EmeraldBullish else CrimsonBearish) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Demo Equity", style = MaterialTheme.typography.labelSmall, color = SoftSlateText)
                        Text(
                            text = String.format(Locale.US, "$%,.2f", startingBalance + realizedPnl),
                            style = MaterialTheme.typography.headlineMedium,
                            color = PureWhite
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Realized P/L", style = MaterialTheme.typography.labelSmall, color = SoftSlateText)
                        Text(
                            text = String.format(Locale.US, "%+.2f USD", realizedPnl),
                            style = MaterialTheme.typography.titleLarge,
                            color = if (realizedPnl >= 0) EmeraldBullish else CrimsonBearish
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricChip("Open Trades", "${openTrades.size}", ElectricCyan)
                    MetricChip("Closed", "${closedTrades.size}", SoftSlateText)
                    MetricChip("Win Rate", "$winRate%", EmeraldBullish)
                }
            }
        }

        // AI Trading Chart Analysis Module
        item {
            NexoraSectionHeader(
                title = "AI Candlestick Chart Analysis",
                subtitle = "চার্ট প্যাটার্ন, ট্রেন্ড, Stop Loss ও Target বিশ্লেষণ",
                icon = Icons.Default.CandlestickChart,
                accentColor = ElectricCyan
            )
            Spacer(modifier = Modifier.height(8.dp))
            NexoraGlassCard(accentColor = ElectricCyan) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onPickChartImage,
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = DeepNavyBackground),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("pick_trading_chart_btn")
                    ) {
                        Icon(Icons.Default.Image, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pick Chart")
                    }
                    OutlinedButton(
                        onClick = onCaptureChartScreen,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("capture_trading_screen_btn")
                    ) {
                        Icon(Icons.Default.ScreenshotMonitor, null, tint = ElectricCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Capture Live", color = PureWhite)
                    }
                }

                if (chartBitmap != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Image(
                        bitmap = chartBitmap.asImageBitmap(),
                        contentDescription = "Selected trading chart",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = symbolInput,
                    onValueChange = { symbolInput = it },
                    label = { Text("Asset / Pair (e.g. BTCUSDT, XAUUSD, EURUSD)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = NavyBorder,
                        focusedContainerColor = NavySurface,
                        unfocusedContainerColor = NavySurface,
                        focusedTextColor = PureWhite,
                        unfocusedTextColor = PureWhite
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("trading_symbol_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("5m", "15m", "1H", "4H", "1D").forEach { tf ->
                        FilterChip(
                            selected = selectedTimeframe == tf,
                            onClick = { selectedTimeframe = tf },
                            label = { Text(tf) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = extraContext,
                    onValueChange = { extraContext = it },
                    placeholder = { Text("প্রাইস লেভেল বা প্রশ্ন লিখুন (ঐচ্ছিক)...", color = MutedSlateText) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VioletNeon,
                        unfocusedBorderColor = NavyBorder,
                        focusedContainerColor = NavySurface,
                        unfocusedContainerColor = NavySurface,
                        focusedTextColor = PureWhite,
                        unfocusedTextColor = PureWhite
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { onAnalyzeChart(symbolInput, selectedTimeframe, extraContext) },
                    colors = ButtonDefaults.buttonColors(containerColor = VioletNeon, contentColor = PureWhite),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("analyze_chart_btn")
                ) {
                    Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Analyze Chart with NEXORA AI")
                }

                when (tradingVisionState) {
                    is VisionUiState.Loading -> {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(color = ElectricCyan, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("ক্যান্ডেলস্টিক প্যাটার্ন ও মার্কেট ট্রেন্ড বিশ্লেষণ চলছে...", color = SoftSlateText)
                        }
                    }

                    is VisionUiState.Error -> {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(tradingVisionState.message, style = MaterialTheme.typography.bodySmall, color = CrimsonBearish)
                    }

                    is VisionUiState.Success -> {
                        val analysis = tradingVisionState.data
                        val trendColor = when (analysis.marketTrend) {
                            "BULLISH" -> EmeraldBullish
                            "BEARISH" -> CrimsonBearish
                            else -> AmberWarning
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = NavyBorder)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(analysis.assetName, style = MaterialTheme.typography.titleMedium, color = PureWhite)
                                Text(analysis.candlestickPattern, style = MaterialTheme.typography.bodySmall, color = ElectricCyan)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                NexoraStatusPill(label = "${analysis.marketTrend} • ${analysis.signalBias}", dotColor = trendColor)
                                IconButton(onClick = { onSpeakText(analysis.bengaliVoiceSummary) }) {
                                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Speak trading summary", tint = ElectricCyan)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(analysis.signalExplanation, style = MaterialTheme.typography.bodyMedium, color = PureWhite)
                        Spacer(modifier = Modifier.height(10.dp))

                        TradingLevelRow("Entry Zone", analysis.entryZone, ElectricCyan)
                        TradingLevelRow("Stop Loss", analysis.stopLossAdvice, CrimsonBearish)
                        TradingLevelRow("Target Zone", analysis.targetAdvice, EmeraldBullish)
                        TradingLevelRow("Risk : Reward", analysis.riskRewardNote, AmberWarning)
                    }

                    VisionUiState.Idle -> Unit
                }
            }
        }

        // Broker API Integration & Live Confirmation Guard
        item {
            NexoraSectionHeader(
                title = "Broker API Integration",
                subtitle = "কাস্টম Broker REST API সংযোগ ও Live Order সুরক্ষা",
                icon = Icons.Default.Api,
                accentColor = VioletNeon
            )
            Spacer(modifier = Modifier.height(8.dp))
            NexoraGlassCard(accentColor = VioletNeon) {
                OutlinedTextField(
                    value = brokerName,
                    onValueChange = { brokerName = it },
                    label = { Text("Broker Name (e.g. Binance, Kite, OANDA)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = NavyBorder,
                        focusedTextColor = PureWhite,
                        unfocusedTextColor = PureWhite
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = brokerUrl,
                    onValueChange = { brokerUrl = it },
                    label = { Text("HTTPS Broker API Base URL") },
                    placeholder = { Text("https://api.example-broker.com/v1/ping", color = MutedSlateText) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = NavyBorder,
                        focusedTextColor = PureWhite,
                        unfocusedTextColor = PureWhite
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onSaveAndTestBroker(brokerName, brokerUrl) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("test_broker_api_btn")
                    ) {
                        if (isTestingBroker) {
                            CircularProgressIndicator(color = ElectricCyan, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                        } else {
                            Text("Test Broker API", color = ElectricCyan)
                        }
                    }
                    Button(
                        onClick = { showLiveConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = AmberWarning, contentColor = DeepNavyBackground),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("live_order_confirm_btn")
                    ) {
                        Text("Live Order Gate")
                    }
                }

                if (brokerHealth != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = brokerHealth.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (brokerHealth.isReachable) EmeraldBullish else CrimsonBearish
                    )
                }
            }
        }

        // Trading History List
        item {
            NexoraSectionHeader(
                title = "Trading History",
                subtitle = "ওপেন এবং ক্লোজড ডেমো ট্রেড তালিকা",
                icon = Icons.Default.CandlestickChart,
                accentColor = EmeraldBullish
            )
        }

        if (demoTrades.isEmpty()) {
            item {
                NexoraGlassCard(accentColor = NavyBorder) {
                    Text(
                        text = "কোনো ডেমো ট্রেড রেকর্ড নেই। '+ Demo Trade' বাটনে চাপ দিয়ে প্র্যাকটিস শুরু করুন।",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SoftSlateText
                    )
                }
            }
        } else {
            items(demoTrades, key = { it.id }) { trade ->
                DemoTradeRow(
                    trade = trade,
                    onCloseClick = { tradeToClose = trade },
                    onDeleteClick = { onDeleteDemoTrade(trade.id) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }

    if (showNewTradeDialog) {
        NewDemoTradeDialog(
            defaultSymbol = symbolInput,
            isLiveFlow = false,
            onDismiss = { showNewTradeDialog = false },
            onConfirm = { sym, side, entry, target, sl, qty, notes ->
                onAddDemoTrade(sym, side, entry, target, sl, qty, notes, false)
                showNewTradeDialog = false
            }
        )
    }

    if (showLiveConfirmDialog) {
        NewDemoTradeDialog(
            defaultSymbol = symbolInput,
            isLiveFlow = true,
            onDismiss = { showLiveConfirmDialog = false },
            onConfirm = { sym, side, entry, target, sl, qty, notes ->
                onAddDemoTrade(sym, side, entry, target, sl, qty, notes, true)
                showLiveConfirmDialog = false
            }
        )
    }

    tradeToClose?.let { closingTrade ->
        CloseTradeDialog(
            trade = closingTrade,
            onDismiss = { tradeToClose = null },
            onConfirmClose = { exitPrice ->
                onCloseDemoTrade(closingTrade, exitPrice)
                tradeToClose = null
            }
        )
    }
}

@Composable
private fun MetricChip(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MutedSlateText)
        Text(value, style = MaterialTheme.typography.titleMedium, color = color)
    }
}

@Composable
private fun TradingLevelRow(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = SoftSlateText)
        Spacer(modifier = Modifier.width(12.dp))
        Text(value, style = MaterialTheme.typography.bodySmall, color = color)
    }
}

@Composable
private fun DemoTradeRow(
    trade: DemoTradeEntity,
    onCloseClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val sideColor = if (trade.side == "BUY") EmeraldBullish else CrimsonBearish
    val pnl = if (trade.status == "CLOSED") trade.computePnl() else null
    NexoraGlassCard(accentColor = sideColor.copy(alpha = 0.6f)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(trade.symbol, style = MaterialTheme.typography.titleMedium, color = PureWhite)
                Spacer(modifier = Modifier.width(8.dp))
                NexoraStatusPill(
                    label = if (trade.isLiveConfirmed) "${trade.side} • CONFIRMED" else "${trade.side} • DEMO",
                    dotColor = sideColor
                )
            }
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete trade", tint = MutedSlateText)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Entry: ${trade.entryPrice} | SL: ${trade.stopLoss} | Target: ${trade.targetPrice} | Qty: ${trade.quantity}",
            style = MaterialTheme.typography.labelMedium,
            color = SoftSlateText
        )
        if (trade.notes.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(trade.notes, style = MaterialTheme.typography.bodySmall, color = MutedSlateText)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (pnl != null) {
                Text(
                    text = String.format(Locale.US, "Closed @ %.2f • P/L: %+.2f USD", trade.exitPrice ?: 0.0, pnl),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (pnl >= 0) EmeraldBullish else CrimsonBearish
                )
            } else {
                Text("Status: OPEN", style = MaterialTheme.typography.labelMedium, color = ElectricCyan)
                OutlinedButton(onClick = onCloseClick, shape = RoundedCornerShape(10.dp)) {
                    Text("Close Trade", color = ElectricCyan)
                }
            }
        }
    }
}

@Composable
private fun NewDemoTradeDialog(
    defaultSymbol: String,
    isLiveFlow: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Double, Double, Double, Double, String) -> Unit
) {
    var symbol by remember { mutableStateOf(defaultSymbol.ifBlank { "BTCUSDT" }) }
    var side by remember { mutableStateOf("BUY") }
    var entry by remember { mutableStateOf("100.0") }
    var sl by remember { mutableStateOf("95.0") }
    var target by remember { mutableStateOf("112.0") }
    var qty by remember { mutableStateOf("1.0") }
    var notes by remember { mutableStateOf("") }
    var riskAccepted by remember { mutableStateOf(!isLiveFlow) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isLiveFlow) "Live Trading Confirmation Required" else "New Demo Trade",
                color = if (isLiveFlow) AmberWarning else ElectricCyan
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isLiveFlow) {
                    Text(
                        "সতর্কতা: লাইভ ট্রেডিংয়ে আর্থিক ক্ষতির ঝুঁকি রয়েছে। NEXORA AI কখনো লাভের গ্যারান্টি দেয় না। অনুমোদন না করলে কোনো অর্ডার প্রস্তুত হবে না।",
                        style = MaterialTheme.typography.bodySmall,
                        color = AmberWarning
                    )
                }
                OutlinedTextField(value = symbol, onValueChange = { symbol = it }, label = { Text("Symbol") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = side == "BUY", onClick = { side = "BUY" }, label = { Text("BUY") })
                    FilterChip(selected = side == "SELL", onClick = { side = "SELL" }, label = { Text("SELL") })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = entry,
                        onValueChange = { entry = it },
                        label = { Text("Entry") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = qty,
                        onValueChange = { qty = it },
                        label = { Text("Qty") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = sl,
                        onValueChange = { sl = it },
                        label = { Text("Stop Loss") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = target,
                        onValueChange = { target = it },
                        label = { Text("Target") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Strategy Note") }, singleLine = true)
                if (isLiveFlow) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = riskAccepted, onCheckedChange = { riskAccepted = it })
                        Text("আমি ঝুঁকি বুঝেছি এবং ম্যানুয়ালি এই অর্ডার কনফার্ম করছি।", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = riskAccepted && symbol.isNotBlank(),
                onClick = {
                    onConfirm(
                        symbol,
                        side,
                        entry.toDoubleOrNull() ?: 100.0,
                        target.toDoubleOrNull() ?: 110.0,
                        sl.toDoubleOrNull() ?: 95.0,
                        qty.toDoubleOrNull() ?: 1.0,
                        notes
                    )
                }
            ) {
                Text(if (isLiveFlow) "Confirm Order" else "Save Demo")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun CloseTradeDialog(
    trade: DemoTradeEntity,
    onDismiss: () -> Unit,
    onConfirmClose: (Double) -> Unit
) {
    var exitText by remember { mutableStateOf(trade.targetPrice.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Close ${trade.symbol} (${trade.side})") },
        text = {
            OutlinedTextField(
                value = exitText,
                onValueChange = { exitText = it },
                label = { Text("Exit Price") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )
        },
        confirmButton = {
            Button(onClick = { onConfirmClose(exitText.toDoubleOrNull() ?: trade.entryPrice) }) {
                Text("Close Position")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
