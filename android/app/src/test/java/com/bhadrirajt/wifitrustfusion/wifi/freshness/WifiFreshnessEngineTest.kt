package com.bhadrirajt.wifitrustfusion.wifi.freshness

import com.bhadrirajt.wifitrustfusion.wifi.model.RawWifiScanObservation
import com.bhadrirajt.wifitrustfusion.wifi.model.ScanCompletionReason
import com.bhadrirajt.wifitrustfusion.wifi.model.ScanRequestRecord
import com.bhadrirajt.wifitrustfusion.wifi.model.TimestampState
import com.bhadrirajt.wifitrustfusion.wifi.scanner.WifiScanBatch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WifiFreshnessEngineTest {

    private fun request(
        requestElapsedUs: Long = 1_000_000L
    ): ScanRequestRecord {

        return ScanRequestRecord(
            sessionId = "TEST_SESSION",
            requestId = "REQ_000001",
            requestUtcIso = "2026-10-11T08:00:00Z",
            requestElapsedUs = requestElapsedUs,
            requestedIntervalMs = 0L,
            startScanReturned = true,
            broadcastReceived = true,
            resultsUpdated = true,
            resultCount = 1,
            completionReason =
                ScanCompletionReason
                    .BROADCAST_RESULTS_UPDATED,
            completedUtcIso =
                "2026-10-11T08:00:01Z",
            completedElapsedUs =
                requestElapsedUs + 1_000_000L
        )
    }

    private fun raw(
        bssid: String? =
            "00:11:22:33:44:55",

        timestampUs: Long? =
            1_200_000L,

        frequencyMhz: Int =
            2412
    ): RawWifiScanObservation {

        return RawWifiScanObservation(
            ssid = "TEST_NETWORK",
            bssid = bssid,
            rssiDbm = -50,
            frequencyMhz = frequencyMhz,
            channelWidth = 0,
            capabilities =
                "[WPA2-PSK-CCMP][ESS]",
            scanTimestampUs =
                timestampUs
        )
    }

    private fun analyze(
        engine: WifiFreshnessEngine,
        rawObservation: RawWifiScanObservation,
        requestElapsedUs: Long =
            1_000_000L,

        observedElapsedUs: Long =
            1_500_000L
    ) = engine.analyzeBatch(
        batch = WifiScanBatch(
            request =
                request(requestElapsedUs),
            observations =
                listOf(rawObservation)
        ),
        observedUtcIso =
            "2026-10-11T08:00:02Z",
        observedElapsedUs =
            observedElapsedUs
    ).single()

    @Test
    fun firstTimestamp_isFirstSeen() {
        val engine =
            WifiFreshnessEngine()

        val observation =
            analyze(
                engine,
                raw(
                    timestampUs =
                        1_200_000L
                )
            )

        assertEquals(
            TimestampState.FIRST_SEEN,
            observation.timestampState
        )
    }

    @Test
    fun newerTimestamp_isAdvanced() {
        val engine =
            WifiFreshnessEngine()

        analyze(
            engine,
            raw(
                timestampUs =
                    1_200_000L
            )
        )

        val second =
            analyze(
                engine,
                raw(
                    timestampUs =
                        1_300_000L
                )
            )

        assertEquals(
            TimestampState.ADVANCED,
            second.timestampState
        )
    }

    @Test
    fun identicalTimestamp_isRepeated() {
        val engine =
            WifiFreshnessEngine()

        analyze(
            engine,
            raw(
                timestampUs =
                    1_200_000L
            )
        )

        val second =
            analyze(
                engine,
                raw(
                    timestampUs =
                        1_200_000L
                )
            )

        assertEquals(
            TimestampState.REPEATED,
            second.timestampState
        )
    }

    @Test
    fun olderTimestamp_isRegressed_andDoesNotReplaceLatestReference() {
        val engine =
            WifiFreshnessEngine()

        analyze(
            engine,
            raw(
                timestampUs =
                    1_200_000L
            )
        )

        analyze(
            engine,
            raw(
                timestampUs =
                    1_400_000L
            )
        )

        val regressed =
            analyze(
                engine,
                raw(
                    timestampUs =
                        1_300_000L
                )
            )

        assertEquals(
            TimestampState.REGRESSED,
            regressed.timestampState
        )

        val latestAgain =
            analyze(
                engine,
                raw(
                    timestampUs =
                        1_400_000L
                )
            )

        assertEquals(
            TimestampState.REPEATED,
            latestAgain.timestampState
        )
    }

    @Test
    fun missingBssid_isUntrackable() {
        val engine =
            WifiFreshnessEngine()

        val observation =
            analyze(
                engine,
                raw(
                    bssid = null,
                    timestampUs =
                        1_200_000L
                )
            )

        assertEquals(
            TimestampState.UNTRACKABLE,
            observation.timestampState
        )
    }

    @Test
    fun scanAgeAndSeenAfterRequest_areCalculatedFromMonotonicTime() {
        val engine =
            WifiFreshnessEngine()

        val observation =
            analyze(
                engine = engine,
                rawObservation =
                    raw(
                        timestampUs =
                            1_200_000L
                    ),
                requestElapsedUs =
                    1_000_000L,
                observedElapsedUs =
                    1_500_000L
            )

        assertEquals(
            300L,
            observation.scanAgeMs
        )

        assertTrue(
            observation.seenAfterRequest == true
        )
    }

    @Test
    fun timestampBeforeRequest_isNotSeenAfterRequest() {
        val engine =
            WifiFreshnessEngine()

        val observation =
            analyze(
                engine = engine,
                rawObservation =
                    raw(
                        timestampUs =
                            900_000L
                    ),
                requestElapsedUs =
                    1_000_000L,
                observedElapsedUs =
                    1_500_000L
            )

        assertFalse(
            observation.seenAfterRequest == true
        )
    }

    @Test
    fun frequency2412_mapsToChannel1() {
        val engine =
            WifiFreshnessEngine()

        val observation =
            analyze(
                engine,
                raw(
                    frequencyMhz = 2412
                )
            )

        assertEquals(
            1,
            observation.channelNumber
        )
    }
}
