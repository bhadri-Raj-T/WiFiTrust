package com.bhadrirajt.wifitrustfusion.wifi.model

data class CollectorSessionMetadata(
    val sessionId: String,
    val collectorVersion: String,
    val protocolVersion: String,
    val observationSchemaVersion: String,

    val appVersionName: String,
    val appVersionCode: Long,

    val deviceAlias: String,
    val manufacturer: String,
    val model: String,
    val deviceCodename: String,
    val androidRelease: String,
    val apiLevel: Int,

    val startedUtcIso: String,
    val startedElapsedUs: Long,

    val requestedScanIntervalMs: Long,

    val runLabel: String? = null
)
