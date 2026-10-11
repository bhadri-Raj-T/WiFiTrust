package com.bhadrirajt.wifitrustfusion

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bhadrirajt.wifitrustfusion.core.ProjectVersions
import com.bhadrirajt.wifitrustfusion.wifi.freshness.WifiFreshnessEngine
import com.bhadrirajt.wifitrustfusion.wifi.model.TimestampState
import com.bhadrirajt.wifitrustfusion.wifi.model.WifiObservation
import com.bhadrirajt.wifitrustfusion.wifi.permission.PermissionCoordinator
import com.bhadrirajt.wifitrustfusion.wifi.scanner.WifiScanBatch
import com.bhadrirajt.wifitrustfusion.wifi.scanner.WifiScanCollector
import java.time.Instant
import java.util.UUID

class MainActivity :
    AppCompatActivity(),
    WifiScanCollector.Listener {

    private lateinit var permissionCoordinator:
        PermissionCoordinator

    private lateinit var scanCollector:
        WifiScanCollector

    private val freshnessEngine =
        WifiFreshnessEngine()

    private lateinit var diagnosticText:
        TextView

    private lateinit var scanStatusText:
        TextView

    private lateinit var scanResultsText:
        TextView

    private lateinit var runScanButton:
        Button

    private val developmentSessionId =
        "M2DEV_" +
            UUID.randomUUID()
                .toString()
                .take(8)
                .uppercase()

    private val locationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts
                .RequestMultiplePermissions()
        ) {
            renderDiagnostics()
        }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(
            savedInstanceState
        )

        setContentView(
            R.layout.activity_main
        )

        permissionCoordinator =
            PermissionCoordinator(
                this
            )

        scanCollector =
            WifiScanCollector(
                context = this,
                permissionCoordinator =
                    permissionCoordinator,
                listener = this
            )

        diagnosticText =
            findViewById(
                R.id.textDiagnostics
            )

        scanStatusText =
            findViewById(
                R.id.textScanStatus
            )

        scanResultsText =
            findViewById(
                R.id.textScanResults
            )

        runScanButton =
            findViewById(
                R.id.buttonRunSingleScan
            )

        findViewById<Button>(
            R.id.buttonRequestPermission
        ).setOnClickListener {
            requestCollectorPermissions()
        }

        findViewById<Button>(
            R.id.buttonLocationSettings
        ).setOnClickListener {
            openLocationSettings()
        }

        findViewById<Button>(
            R.id.buttonRefresh
        ).setOnClickListener {
            renderDiagnostics()
        }

        runScanButton.setOnClickListener {
            runSingleScan()
        }

        renderDiagnostics()
    }

    override fun onStart() {
        super.onStart()

        scanCollector.register()
    }

    override fun onResume() {
        super.onResume()

        if (
            ::permissionCoordinator.isInitialized
        ) {
            renderDiagnostics()
        }
    }

    override fun onStop() {
        if (
            ::scanCollector.isInitialized
        ) {
            scanCollector.unregister()
        }

        super.onStop()
    }

    private fun requestCollectorPermissions() {
        val snapshot =
            permissionCoordinator.snapshot()

        if (
            snapshot.fineLocationGranted
        ) {
            Toast.makeText(
                this,
                "Required location permission is already granted.",
                Toast.LENGTH_SHORT
            ).show()

            renderDiagnostics()

            return
        }

        locationPermissionLauncher.launch(
            permissionCoordinator
                .requiredRuntimePermissions()
        )
    }

    private fun openLocationSettings() {
        try {
            startActivity(
                Intent(
                    Settings
                        .ACTION_LOCATION_SOURCE_SETTINGS
                )
            )
        } catch (_: Exception) {
            Toast.makeText(
                this,
                "Unable to open Location settings.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun runSingleScan() {
        scanResultsText.text =
            "Waiting for scan result..."

        scanCollector.requestSingleScan(
            sessionId =
                developmentSessionId,
            requestedIntervalMs =
                0L
        )

        updateScanButton()
    }

    override fun onScanCompleted(
        batch: WifiScanBatch
    ) {
        runOnUiThread {

            val analyzedObservations =
                freshnessEngine.analyzeBatch(
                    batch =
                        batch,

                    observedUtcIso =
                        Instant.now()
                            .toString(),

                    observedElapsedUs =
                        SystemClock
                            .elapsedRealtimeNanos() /
                            1_000L
                )

            renderBatch(
                batch =
                    batch,
                observations =
                    analyzedObservations
            )

            renderDiagnostics()

            updateScanButton()
        }
    }

    override fun onCollectorStatus(
        message: String
    ) {
        runOnUiThread {

            scanStatusText.text =
                "Collector status:\n$message"

            updateScanButton()
        }
    }

    private fun renderBatch(
        batch: WifiScanBatch,
        observations: List<WifiObservation>
    ) {
        val request =
            batch.request

        val firstSeenCount =
            observations.count {
                it.timestampState ==
                    TimestampState.FIRST_SEEN
            }

        val advancedCount =
            observations.count {
                it.timestampState ==
                    TimestampState.ADVANCED
            }

        val repeatedCount =
            observations.count {
                it.timestampState ==
                    TimestampState.REPEATED
            }

        val regressedCount =
            observations.count {
                it.timestampState ==
                    TimestampState.REGRESSED
            }

        val untrackableCount =
            observations.count {
                it.timestampState ==
                    TimestampState.UNTRACKABLE
            }

        val seenAfterRequestCount =
            observations.count {
                it.seenAfterRequest == true
            }

        scanResultsText.text =
            buildString {

                appendLine("REQUEST")

                appendLine(
                    "Session: ${request.sessionId}"
                )

                appendLine(
                    "Request: ${request.requestId}"
                )

                appendLine(
                    "startScan(): ${request.startScanReturned}"
                )

                appendLine(
                    "Broadcast: ${request.broadcastReceived}"
                )

                appendLine(
                    "Results updated: ${request.resultsUpdated}"
                )

                appendLine(
                    "Result count: ${request.resultCount}"
                )

                appendLine(
                    "Completion: ${request.completionReason}"
                )

                if (
                    request.failureDetail != null
                ) {
                    appendLine(
                        "Detail: ${request.failureDetail}"
                    )
                }

                appendLine()

                appendLine(
                    "FRESHNESS SUMMARY"
                )

                appendLine(
                    "FIRST_SEEN: $firstSeenCount"
                )

                appendLine(
                    "ADVANCED: $advancedCount"
                )

                appendLine(
                    "REPEATED: $repeatedCount"
                )

                appendLine(
                    "REGRESSED: $regressedCount"
                )

                appendLine(
                    "UNTRACKABLE: $untrackableCount"
                )

                appendLine(
                    "Seen after request: $seenAfterRequestCount / ${observations.size}"
                )

                appendLine()

                appendLine(
                    "OBSERVATIONS"
                )

                if (
                    observations.isEmpty()
                ) {
                    appendLine(
                        "No observations assigned to this request."
                    )
                }

                observations
                    .sortedByDescending {
                        it.rssiDbm
                    }
                    .take(12)
                    .forEachIndexed {
                            index,
                            observation ->

                        appendLine()

                        appendLine(
                            "AP ${index + 1}"
                        )

                        appendLine(
                            "RSSI: ${observation.rssiDbm} dBm"
                        )

                        appendLine(
                            "Frequency: ${observation.frequencyMhz} MHz"
                        )

                        appendLine(
                            "Channel: ${
                                observation.channelNumber
                                    ?: "unknown"
                            }"
                        )

                        appendLine(
                            "Timestamp state: ${observation.timestampState}"
                        )

                        appendLine(
                            "Scan age: ${
                                observation.scanAgeMs
                                    ?.let { "$it ms" }
                                    ?: "unknown"
                            }"
                        )

                        appendLine(
                            "Seen after request: ${
                                observation.seenAfterRequest
                                    ?: "unknown"
                            }"
                        )
                    }
            }
    }

    private fun renderDiagnostics() {
        val permission =
            permissionCoordinator.snapshot()

        val wifiEnabled =
            if (
                ::scanCollector.isInitialized
            ) {
                scanCollector.isWifiEnabled()
            } else {
                false
            }

        val ready =
            permission.readyForWifiCollection &&
                wifiEnabled

        diagnosticText.text =
            buildString {

                appendLine("PROJECT")
                appendLine("WiFiTrustFusion")
                appendLine()

                appendLine(
                    "IMPLEMENTATION STATUS"
                )

                appendLine(
                    "Milestone: ${ProjectVersions.MILESTONE}"
                )

                appendLine(
                    "Collector: ${ProjectVersions.COLLECTOR_VERSION}"
                )

                appendLine(
                    "Protocol: ${ProjectVersions.PROTOCOL_VERSION}"
                )

                appendLine(
                    "Observation schema: ${ProjectVersions.OBS_SCHEMA_VERSION}"
                )

                appendLine()

                appendLine(
                    "COLLECTOR PREREQUISITES"
                )

                appendLine(
                    "Fine location: ${permission.fineLocationGranted}"
                )

                appendLine(
                    "Coarse location: ${permission.coarseLocationGranted}"
                )

                appendLine(
                    "Location services: ${permission.locationServicesEnabled}"
                )

                appendLine(
                    "Wi-Fi enabled: $wifiEnabled"
                )

                appendLine(
                    "Ready: $ready"
                )

                appendLine()

                appendLine(
                    "M2.5 BOUNDARY"
                )

                appendLine(
                    "Single Wi-Fi scan: IMPLEMENTED"
                )

                appendLine(
                    "Raw observations: IMPLEMENTED"
                )

                appendLine(
                    "Timestamp freshness: IMPLEMENTED"
                )

                appendLine(
                    "Scan age: IMPLEMENTED"
                )

                appendLine(
                    "seenAfterRequest: IMPLEMENTED"
                )

                appendLine(
                    "Repeated acquisition: NOT IMPLEMENTED"
                )

                appendLine(
                    "Persistent logging: NOT IMPLEMENTED"
                )

                appendLine(
                    "Risk scoring: NOT IMPLEMENTED"
                )
            }

        updateScanButton()
    }

    private fun updateScanButton() {
        if (
            !::runScanButton.isInitialized
        ) {
            return
        }

        runScanButton.isEnabled =
            !scanCollector
                .isRequestInProgress()
    }
}
