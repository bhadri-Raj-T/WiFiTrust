package com.bhadrirajt.wifitrustfusion.wifi.model

data class CollectorEventRecord(
    val eventId: String,
    val sessionId: String,

    val eventUtcIso: String,
    val eventElapsedUs: Long,

    val eventType: CollectorEventType,

    val requestId: String? = null,
    val detail: String? = null
)
