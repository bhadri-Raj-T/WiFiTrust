package com.bhadrirajt.wifitrustfusion.wifi.model

data class WifiObservation(
    val observationId: String,

    val sessionId: String,
    val requestId: String,

    val observedUtcIso: String,
    val observedElapsedUs: Long,

    val ssid: String?,
    val bssid: String?,

    val rssiDbm: Int,
    val frequencyMhz: Int,
    val channelNumber: Int?,
    val channelWidth: Int?,

    val capabilities: String,

    val scanTimestampUs: Long?,
    val scanAgeMs: Long?,

    val timestampState: TimestampState,

    val seenAfterRequest: Boolean?
)
