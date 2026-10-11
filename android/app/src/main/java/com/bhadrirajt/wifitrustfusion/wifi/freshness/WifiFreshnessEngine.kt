package com.bhadrirajt.wifitrustfusion.wifi.freshness

import com.bhadrirajt.wifitrustfusion.wifi.model.RawWifiScanObservation
import com.bhadrirajt.wifitrustfusion.wifi.model.TimestampState
import com.bhadrirajt.wifitrustfusion.wifi.model.WifiObservation
import com.bhadrirajt.wifitrustfusion.wifi.scanner.WifiScanBatch

class WifiFreshnessEngine {

    private val latestTimestampByBssid =
        mutableMapOf<String, Long>()

    fun reset() {
        latestTimestampByBssid.clear()
    }

    fun analyzeBatch(
        batch: WifiScanBatch,
        observedUtcIso: String,
        observedElapsedUs: Long
    ): List<WifiObservation> {

        return batch.observations.mapIndexed {
                index,
                rawObservation ->

            analyzeObservation(
                rawObservation = rawObservation,
                observationIndex = index,
                batch = batch,
                observedUtcIso = observedUtcIso,
                observedElapsedUs = observedElapsedUs
            )
        }
    }

    private fun analyzeObservation(
        rawObservation: RawWifiScanObservation,
        observationIndex: Int,
        batch: WifiScanBatch,
        observedUtcIso: String,
        observedElapsedUs: Long
    ): WifiObservation {

        val timestampState =
            determineTimestampState(
                bssid = rawObservation.bssid,
                timestampUs =
                    rawObservation.scanTimestampUs
            )

        val scanAgeMs =
            rawObservation.scanTimestampUs
                ?.let { timestampUs ->
                    (
                        observedElapsedUs -
                            timestampUs
                    ) / 1_000L
                }

        val seenAfterRequest =
            rawObservation.scanTimestampUs
                ?.let { timestampUs ->
                    timestampUs >=
                        batch.request.requestElapsedUs
                }

        return WifiObservation(
            observationId =
                "${batch.request.requestId}_OBS_%03d"
                    .format(observationIndex + 1),

            sessionId =
                batch.request.sessionId,

            requestId =
                batch.request.requestId,

            observedUtcIso =
                observedUtcIso,

            observedElapsedUs =
                observedElapsedUs,

            ssid =
                rawObservation.ssid,

            bssid =
                rawObservation.bssid,

            rssiDbm =
                rawObservation.rssiDbm,

            frequencyMhz =
                rawObservation.frequencyMhz,

            channelNumber =
                frequencyToChannel(
                    rawObservation.frequencyMhz
                ),

            channelWidth =
                rawObservation.channelWidth,

            capabilities =
                rawObservation.capabilities,

            scanTimestampUs =
                rawObservation.scanTimestampUs,

            scanAgeMs =
                scanAgeMs,

            timestampState =
                timestampState,

            seenAfterRequest =
                seenAfterRequest
        )
    }

    private fun determineTimestampState(
        bssid: String?,
        timestampUs: Long?
    ): TimestampState {

        if (
            bssid.isNullOrBlank() ||
            timestampUs == null ||
            timestampUs <= 0L
        ) {
            return TimestampState.UNTRACKABLE
        }

        val normalizedBssid =
            bssid.trim().lowercase()

        val previousTimestamp =
            latestTimestampByBssid[
                normalizedBssid
            ]

        if (previousTimestamp == null) {
            latestTimestampByBssid[
                normalizedBssid
            ] = timestampUs

            return TimestampState.FIRST_SEEN
        }

        return when {
            timestampUs > previousTimestamp -> {
                latestTimestampByBssid[
                    normalizedBssid
                ] = timestampUs

                TimestampState.ADVANCED
            }

            timestampUs == previousTimestamp -> {
                TimestampState.REPEATED
            }

            else -> {
                /*
                 * Do NOT replace the last known newer timestamp
                 * with a regressed timestamp.
                 *
                 * This preserves the strongest freshness reference
                 * for subsequent observations.
                 */
                TimestampState.REGRESSED
            }
        }
    }

    private fun frequencyToChannel(
        frequencyMhz: Int
    ): Int? {

        return when {
            frequencyMhz == 2484 ->
                14

            frequencyMhz in 2412..2472 ->
                (frequencyMhz - 2407) / 5

            frequencyMhz in 5000..5895 ->
                (frequencyMhz - 5000) / 5

            frequencyMhz in 5955..7115 ->
                (frequencyMhz - 5950) / 5

            else ->
                null
        }
    }
}
