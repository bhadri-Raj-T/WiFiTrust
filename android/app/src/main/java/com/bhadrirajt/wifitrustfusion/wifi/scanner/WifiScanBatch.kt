package com.bhadrirajt.wifitrustfusion.wifi.scanner

import com.bhadrirajt.wifitrustfusion.wifi.model.RawWifiScanObservation
import com.bhadrirajt.wifitrustfusion.wifi.model.ScanRequestRecord

data class WifiScanBatch(
    val request: ScanRequestRecord,
    val observations: List<RawWifiScanObservation>
)
