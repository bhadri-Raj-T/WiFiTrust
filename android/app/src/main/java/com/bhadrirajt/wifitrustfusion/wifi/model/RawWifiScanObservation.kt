package com.bhadrirajt.wifitrustfusion.wifi.model

data class RawWifiScanObservation(
    val ssid: String?,
    val bssid: String?,
    val rssiDbm: Int,
    val frequencyMhz: Int,
    val channelWidth: Int?,
    val capabilities: String,
    val scanTimestampUs: Long?
)
