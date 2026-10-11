package com.bhadrirajt.wifitrustfusion.wifi.model

data class ScanRequestRecord(
    val sessionId: String,
    val requestId: String,

    val requestUtcIso: String,
    val requestElapsedUs: Long,

    val requestedIntervalMs: Long,

    val startScanReturned: Boolean?,

    val broadcastReceived: Boolean,
    val resultsUpdated: Boolean?,

    val resultCount: Int,

    val completionReason: ScanCompletionReason,

    val completedUtcIso: String?,
    val completedElapsedUs: Long?,

    val failureDetail: String? = null
)
