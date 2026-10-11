package com.bhadrirajt.wifitrustfusion.wifi.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ObservationModelsTest {

    @Test
    fun timestampState_containsRequiredStates() {
        val states = TimestampState.entries.map { it.name }

        assertTrue(states.contains("FIRST_SEEN"))
        assertTrue(states.contains("ADVANCED"))
        assertTrue(states.contains("REPEATED"))
        assertTrue(states.contains("REGRESSED"))
        assertTrue(states.contains("UNTRACKABLE"))
    }

    @Test
    fun requestRecord_canRepresentFailedScanWithoutObservation() {
        val request = ScanRequestRecord(
            sessionId = "J7_TEST_SESSION",
            requestId = "REQ_000001",
            requestUtcIso = "2026-10-11T06:30:00Z",
            requestElapsedUs = 1_000_000L,
            requestedIntervalMs = 30_000L,
            startScanReturned = false,
            broadcastReceived = false,
            resultsUpdated = null,
            resultCount = 0,
            completionReason =
                ScanCompletionReason.START_SCAN_RETURNED_FALSE,
            completedUtcIso = "2026-10-11T06:30:00Z",
            completedElapsedUs = 1_010_000L
        )

        assertFalse(request.startScanReturned ?: true)
        assertFalse(request.broadcastReceived)
        assertEquals(0, request.resultCount)
        assertEquals(
            ScanCompletionReason.START_SCAN_RETURNED_FALSE,
            request.completionReason
        )
    }

    @Test
    fun requestRecord_canRepresentPermissionBlockWithoutCallingStartScan() {
        val request = ScanRequestRecord(
            sessionId = "J7_TEST_SESSION",
            requestId = "REQ_000002",
            requestUtcIso = "2026-10-11T06:31:00Z",
            requestElapsedUs = 2_000_000L,
            requestedIntervalMs = 30_000L,
            startScanReturned = null,
            broadcastReceived = false,
            resultsUpdated = null,
            resultCount = 0,
            completionReason =
                ScanCompletionReason.PERMISSION_MISSING,
            completedUtcIso = "2026-10-11T06:31:00Z",
            completedElapsedUs = 2_001_000L
        )

        assertNull(request.startScanReturned)
        assertFalse(request.broadcastReceived)
        assertEquals(0, request.resultCount)
    }

    @Test
    fun observation_keepsNativeAndCollectorTimeSeparate() {
        val observation = WifiObservation(
            observationId = "OBS_000001",
            sessionId = "J7_TEST_SESSION",
            requestId = "REQ_000003",
            observedUtcIso = "2026-10-11T06:32:00Z",
            observedElapsedUs = 10_500_000L,
            ssid = "TEST_NETWORK",
            bssid = "00:00:00:00:00:00",
            rssiDbm = -52,
            frequencyMhz = 2412,
            channelNumber = 1,
            channelWidth = 0,
            capabilities = "[WPA2-PSK-CCMP][ESS]",
            scanTimestampUs = 10_200_000L,
            scanAgeMs = 300L,
            timestampState = TimestampState.ADVANCED,
            seenAfterRequest = true
        )

        assertEquals(10_500_000L, observation.observedElapsedUs)
        assertEquals(10_200_000L, observation.scanTimestampUs)
        assertEquals(300L, observation.scanAgeMs)
        assertEquals(TimestampState.ADVANCED, observation.timestampState)
        assertTrue(observation.seenAfterRequest == true)
    }
}
