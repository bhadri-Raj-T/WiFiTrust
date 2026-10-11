package com.bhadrirajt.wifitrustfusion

import android.content.Intent
import android.os.Build
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
import com.bhadrirajt.wifitrustfusion.wifi.logging.ResearchSessionLogger
import com.bhadrirajt.wifitrustfusion.wifi.model.CollectorEventRecord
import com.bhadrirajt.wifitrustfusion.wifi.model.CollectorEventType
import com.bhadrirajt.wifitrustfusion.wifi.model.CollectorSessionMetadata
import com.bhadrirajt.wifitrustfusion.wifi.model.ScanCompletionReason
import com.bhadrirajt.wifitrustfusion.wifi.model.TimestampState
import com.bhadrirajt.wifitrustfusion.wifi.model.WifiObservation
import com.bhadrirajt.wifitrustfusion.wifi.permission.PermissionCoordinator
import com.bhadrirajt.wifitrustfusion.wifi.scanner.WifiScanBatch
import com.bhadrirajt.wifitrustfusion.wifi.scanner.WifiScanCollector
import com.bhadrirajt.wifitrustfusion.wifi.session.CollectorSessionController
import java.io.File
import java.time.Instant

class MainActivity :
    AppCompatActivity(),
    WifiScanCollector.Listener,
    CollectorSessionController.Listener {

    companion object {

        private const val DEVICE_ALIAS =
            "J7"
    }

    private lateinit var permissionCoordinator:
        PermissionCoordinator

    private lateinit var scanCollector:
        WifiScanCollector

    private lateinit var sessionController:
        CollectorSessionController

    private lateinit var researchLogger:
        ResearchSessionLogger

    private val freshnessEngine =
        WifiFreshnessEngine()

    private lateinit var diagnosticText:
        TextView

    private lateinit var sessionText:
        TextView

    private lateinit var scanStatusText:
        TextView

    private lateinit var scanResultsText:
        TextView

    private lateinit var startSessionButton:
        Button

    private lateinit var stopSessionButton:
        Button

    private lateinit var singleScanButton:
        Button

    private var latestRequestId:
        String? = null

    private var latestResultCount =
        0

    private var latestFirstSeen =
        0

    private var latestAdvanced =
        0

    private var latestRepeated =
        0

    private var latestRegressed =
        0

    private var latestUntrackable =
        0

    private var latestSeenAfterRequest =
        0

    private var eventCounter =
        0L

    private var sessionLoggingReady =
        false

    private var currentEvidenceDirectory:
        File? = null

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
                context =
                    this,
                permissionCoordinator =
                    permissionCoordinator,
                listener =
                    this
            )

        sessionController =
            CollectorSessionController(
                listener =
                    this
            )

        researchLogger =
            ResearchSessionLogger(
                this
            )

        diagnosticText =
            findViewById(
                R.id.textDiagnostics
            )

        sessionText =
            findViewById(
                R.id.textSession
            )

        scanStatusText =
            findViewById(
                R.id.textScanStatus
            )

        scanResultsText =
            findViewById(
                R.id.textScanResults
            )

        startSessionButton =
            findViewById(
                R.id.buttonStartSession
            )

        stopSessionButton =
            findViewById(
                R.id.buttonStopSession
            )

        singleScanButton =
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
            renderSessionStatus()
        }

        singleScanButton
            .setOnClickListener {

                runDevelopmentSingleScan()
            }

        startSessionButton
            .setOnClickListener {

                startCollectorSession()
            }

        stopSessionButton
            .setOnClickListener {

                stopCollectorSession()
            }

        renderDiagnostics()
        renderSessionStatus()
        updateButtons()
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
            renderSessionStatus()
        }
    }

    override fun onStop() {

        /*
         * Finish any pending request first while the logger
         * and session are still active.
         */
        if (
            ::scanCollector.isInitialized
        ) {
            scanCollector.unregister()
        }

        if (
            ::sessionController.isInitialized &&
            sessionController.isRunning()
        ) {
            sessionController.stopSession(
                reason =
                    "ACTIVITY_LEFT_FOREGROUND"
            )
        }

        super.onStop()
    }

    override fun onDestroy() {

        if (
            ::researchLogger.isInitialized
        ) {
            researchLogger.close()
        }

        if (
            ::sessionController.isInitialized
        ) {
            sessionController.destroy()
        }

        super.onDestroy()
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

    private fun prerequisitesReady():
        Boolean {

        val permission =
            permissionCoordinator.snapshot()

        return permission
            .readyForWifiCollection &&
            scanCollector.isWifiEnabled()
    }

    private fun runDevelopmentSingleScan() {

        if (
            sessionController.isRunning()
        ) {

            Toast.makeText(
                this,
                "Stop the running session before using manual single-scan mode.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        if (
            !prerequisitesReady()
        ) {

            renderDiagnostics()

            Toast.makeText(
                this,
                "Collector prerequisites are not ready.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        scanResultsText.text =
            "Waiting for manual scan..."

        scanCollector.requestSingleScan(
            sessionId =
                "M2_MANUAL",
            requestedIntervalMs =
                0L
        )

        updateButtons()
    }

    private fun startCollectorSession() {

        if (
            !prerequisitesReady()
        ) {

            renderDiagnostics()

            Toast.makeText(
                this,
                "Collector prerequisites are not ready.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        freshnessEngine.reset()

        resetLatestSummary()

        eventCounter =
            0L

        sessionLoggingReady =
            false

        currentEvidenceDirectory =
            null

        sessionController.startSession(
            deviceAlias =
                DEVICE_ALIAS,
            intervalMs =
                CollectorSessionController
                    .DEFAULT_DEV_INTERVAL_MS
        )

        renderSessionStatus()
        updateButtons()
    }

    private fun stopCollectorSession() {

        if (
            !sessionController.isRunning()
        ) {
            return
        }

        sessionController.stopSession(
            reason =
                "USER_STOP"
        )

        renderSessionStatus()
        updateButtons()
    }

    override fun onSessionStarted(
        sessionId: String,
        intervalMs: Long
    ) {

        runOnUiThread {

            val metadata =
                CollectorSessionMetadata(
                    sessionId =
                        sessionId,

                    collectorVersion =
                        ProjectVersions
                            .COLLECTOR_VERSION,

                    protocolVersion =
                        ProjectVersions
                            .PROTOCOL_VERSION,

                    observationSchemaVersion =
                        ProjectVersions
                            .OBS_SCHEMA_VERSION,

                    appVersionName =
                        getAppVersionName(),

                    appVersionCode =
                        getAppVersionCode(),

                    deviceAlias =
                        DEVICE_ALIAS,

                    manufacturer =
                        Build.MANUFACTURER,

                    model =
                        Build.MODEL,

                    deviceCodename =
                        Build.DEVICE,

                    androidRelease =
                        Build.VERSION.RELEASE,

                    apiLevel =
                        Build.VERSION.SDK_INT,

                    startedUtcIso =
                        Instant.now()
                            .toString(),

                    startedElapsedUs =
                        elapsedRealtimeUs(),

                    requestedScanIntervalMs =
                        intervalMs,

                    runLabel =
                        "M2.7_DEV"
                )

            try {

                currentEvidenceDirectory =
                    researchLogger
                        .startSession(
                            metadata
                        )

                sessionLoggingReady =
                    true

                logEvent(
                    eventType =
                        CollectorEventType
                            .SESSION_STARTED,
                    requestId =
                        null,
                    detail =
                        "Persistent session evidence initialized."
                )

            } catch (
                exception: Exception
            ) {

                sessionLoggingReady =
                    false

                scanStatusText.text =
                    "LOGGER START ERROR:\n" +
                        (
                            exception.message
                                ?: exception
                                    .javaClass
                                    .simpleName
                        )
            }

            renderSessionStatus()
            updateButtons()
        }
    }

    override fun onScanDue(
        sessionId: String,
        intervalMs: Long
    ) {

        runOnUiThread {

            if (
                !sessionLoggingReady
            ) {

                if (
                    sessionController.isRunning()
                ) {
                    sessionController
                        .stopSession(
                            reason =
                                "LOGGER_START_FAILED"
                        )
                }

                return@runOnUiThread
            }

            if (
                scanCollector
                    .isRequestInProgress()
            ) {

                onSessionStatus(
                    "Scan due but another request is still in progress."
                )

                return@runOnUiThread
            }

            logEventSafely(
                eventType =
                    CollectorEventType
                        .SCAN_REQUEST_CREATED,
                requestId =
                    null,
                detail =
                    "Scheduled scan became due."
            )

            scanCollector.requestSingleScan(
                sessionId =
                    sessionId,
                requestedIntervalMs =
                    intervalMs
            )

            updateButtons()
        }
    }

    override fun onSessionStopped(
        sessionId: String,
        completedScanCount: Int,
        reason: String
    ) {

        runOnUiThread {

            if (
                sessionLoggingReady
            ) {

                try {

                    logEvent(
                        eventType =
                            CollectorEventType
                                .SESSION_STOPPED,
                        requestId =
                            null,
                        detail =
                            "reason=$reason; completedScans=$completedScanCount"
                    )

                    researchLogger
                        .markSessionComplete(
                            stopReason =
                                reason,
                            completedScanCount =
                                completedScanCount
                        )

                } catch (
                    exception: Exception
                ) {

                    scanStatusText.text =
                        "LOGGER CLOSE ERROR:\n" +
                            (
                                exception.message
                                    ?: exception
                                        .javaClass
                                        .simpleName
                            )
                }
            }

            researchLogger.close()

            sessionLoggingReady =
                false

            sessionText.text =
                buildString {

                    appendLine(
                        "SESSION STOPPED"
                    )

                    appendLine(
                        "Session: $sessionId"
                    )

                    appendLine(
                        "Completed scans: $completedScanCount"
                    )

                    appendLine(
                        "Stop reason: $reason"
                    )

                    appendLine()

                    appendLine(
                        "Evidence:"
                    )

                    appendLine(
                        currentEvidenceDirectory
                            ?.absolutePath
                            ?: "unavailable"
                    )
                }

            updateButtons()
        }
    }

    override fun onSessionStatus(
        message: String
    ) {

        runOnUiThread {

            scanStatusText.text =
                "Session status:\n$message"

            renderSessionStatus()

            updateButtons()
        }
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
                        elapsedRealtimeUs()
                )

            renderBatch(
                batch =
                    batch,
                observations =
                    analyzedObservations
            )

            val activeSessionId =
                sessionController
                    .getCurrentSessionId()

            val belongsToActiveSession =
                sessionController.isRunning() &&
                    activeSessionId != null &&
                    batch.request.sessionId ==
                        activeSessionId

            if (
                belongsToActiveSession &&
                sessionLoggingReady
            ) {

                try {

                    researchLogger
                        .logRequest(
                            batch.request
                        )

                    researchLogger
                        .logObservations(
                            analyzedObservations
                        )

                    logCompletionEvent(
                        batch
                    )

                } catch (
                    exception: Exception
                ) {

                    scanStatusText.text =
                        "LOGGER WRITE ERROR:\n" +
                            (
                                exception.message
                                    ?: exception
                                        .javaClass
                                        .simpleName
                            )

                    sessionLoggingReady =
                        false

                    if (
                        sessionController
                            .isRunning()
                    ) {
                        sessionController
                            .stopSession(
                                reason =
                                    "LOGGER_WRITE_FAILED"
                            )
                    }

                    return@runOnUiThread
                }
            }

            if (
                belongsToActiveSession &&
                sessionController.isRunning()
            ) {

                sessionController
                    .notifyScanCompleted()
            }

            renderDiagnostics()
            renderSessionStatus()
            updateButtons()
        }
    }

    override fun onCollectorStatus(
        message: String
    ) {

        runOnUiThread {

            scanStatusText.text =
                "Collector status:\n$message"

            updateButtons()
        }
    }

    private fun logCompletionEvent(
        batch: WifiScanBatch
    ) {

        val request =
            batch.request

        val eventType =
            when (
                request.completionReason
            ) {

                ScanCompletionReason
                    .BROADCAST_RESULTS_UPDATED,

                ScanCompletionReason
                    .BROADCAST_RESULTS_NOT_UPDATED ->

                    CollectorEventType
                        .SCAN_BROADCAST_RECEIVED

                ScanCompletionReason
                    .SCAN_TIMEOUT ->

                    CollectorEventType
                        .SCAN_TIMEOUT

                ScanCompletionReason
                    .PERMISSION_MISSING,

                ScanCompletionReason
                    .WIFI_DISABLED,

                ScanCompletionReason
                    .LOCATION_DISABLED ->

                    CollectorEventType
                        .PREREQUISITE_BLOCKED

                ScanCompletionReason
                    .SECURITY_EXCEPTION,

                ScanCompletionReason
                    .COLLECTOR_ERROR ->

                    CollectorEventType
                        .COLLECTOR_ERROR

                else ->

                    CollectorEventType
                        .START_SCAN_CALLED
            }

        logEvent(
            eventType =
                eventType,
            requestId =
                request.requestId,
            detail =
                "completion=${request.completionReason}; " +
                    "resultCount=${request.resultCount}; " +
                    "resultsUpdated=${request.resultsUpdated}"
        )
    }

    private fun logEventSafely(
        eventType: CollectorEventType,
        requestId: String?,
        detail: String?
    ) {

        if (
            !sessionLoggingReady
        ) {
            return
        }

        try {

            logEvent(
                eventType =
                    eventType,
                requestId =
                    requestId,
                detail =
                    detail
            )

        } catch (
            exception: Exception
        ) {

            scanStatusText.text =
                "LOGGER EVENT ERROR:\n" +
                    (
                        exception.message
                            ?: exception
                                .javaClass
                                .simpleName
                    )
        }
    }

    private fun logEvent(
        eventType: CollectorEventType,
        requestId: String?,
        detail: String?
    ) {

        val sessionId =
            sessionController
                .getCurrentSessionId()
                ?: return

        eventCounter +=
            1L

        researchLogger.logEvent(
            CollectorEventRecord(
                eventId =
                    "EVT_%06d"
                        .format(
                            eventCounter
                        ),

                sessionId =
                    sessionId,

                eventUtcIso =
                    Instant.now()
                        .toString(),

                eventElapsedUs =
                    elapsedRealtimeUs(),

                eventType =
                    eventType,

                requestId =
                    requestId,

                detail =
                    detail
            )
        )
    }

    private fun renderBatch(
        batch: WifiScanBatch,
        observations: List<WifiObservation>
    ) {

        val request =
            batch.request

        latestRequestId =
            request.requestId

        latestResultCount =
            observations.size

        latestFirstSeen =
            observations.count {
                it.timestampState ==
                    TimestampState.FIRST_SEEN
            }

        latestAdvanced =
            observations.count {
                it.timestampState ==
                    TimestampState.ADVANCED
            }

        latestRepeated =
            observations.count {
                it.timestampState ==
                    TimestampState.REPEATED
            }

        latestRegressed =
            observations.count {
                it.timestampState ==
                    TimestampState.REGRESSED
            }

        latestUntrackable =
            observations.count {
                it.timestampState ==
                    TimestampState.UNTRACKABLE
            }

        latestSeenAfterRequest =
            observations.count {
                it.seenAfterRequest == true
            }

        scanResultsText.text =
            buildString {

                appendLine(
                    "LATEST REQUEST"
                )

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

                appendLine()

                appendLine(
                    "FRESHNESS SUMMARY"
                )

                appendLine(
                    "FIRST_SEEN: $latestFirstSeen"
                )

                appendLine(
                    "ADVANCED: $latestAdvanced"
                )

                appendLine(
                    "REPEATED: $latestRepeated"
                )

                appendLine(
                    "REGRESSED: $latestRegressed"
                )

                appendLine(
                    "UNTRACKABLE: $latestUntrackable"
                )

                appendLine(
                    "Seen after request: $latestSeenAfterRequest / ${observations.size}"
                )

                appendLine()

                appendLine(
                    "Privacy-safe display"
                )

                appendLine(
                    "Raw SSID/BSSID values are stored only in app-specific research evidence files."
                )
            }
    }

    private fun renderSessionStatus() {

        if (
            !::sessionController.isInitialized
        ) {
            return
        }

        if (
            sessionController.isRunning()
        ) {

            sessionText.text =
                buildString {

                    appendLine(
                        "SESSION RUNNING"
                    )

                    appendLine(
                        "Session: ${
                            sessionController
                                .getCurrentSessionId()
                        }"
                    )

                    appendLine(
                        "Device alias: $DEVICE_ALIAS"
                    )

                    appendLine(
                        "Development interval: ${
                            sessionController
                                .getIntervalMs() /
                                1_000L
                        } s"
                    )

                    appendLine(
                        "Completed scans: ${
                            sessionController
                                .getCompletedScanCount()
                        }"
                    )

                    appendLine(
                        "Persistent logging: ${
                            if (
                                sessionLoggingReady
                            ) {
                                "ACTIVE"
                            } else {
                                "NOT READY"
                            }
                        }"
                    )

                    if (
                        currentEvidenceDirectory !=
                        null
                    ) {

                        appendLine()

                        appendLine(
                            "Evidence directory:"
                        )

                        appendLine(
                            currentEvidenceDirectory
                                ?.absolutePath
                        )
                    }

                    if (
                        latestRequestId != null
                    ) {

                        appendLine()

                        appendLine(
                            "Latest request: $latestRequestId"
                        )

                        appendLine(
                            "Latest result count: $latestResultCount"
                        )
                    }
                }

        } else if (
            sessionText.text
                .toString()
                .isBlank()
        ) {

            sessionText.text =
                "SESSION IDLE"
        }
    }

    private fun renderDiagnostics() {

        val permission =
            permissionCoordinator.snapshot()

        val wifiEnabled =
            if (
                ::scanCollector.isInitialized
            ) {
                scanCollector
                    .isWifiEnabled()
            } else {
                false
            }

        val ready =
            permission
                .readyForWifiCollection &&
                wifiEnabled

        diagnosticText.text =
            buildString {

                appendLine(
                    "PROJECT"
                )

                appendLine(
                    "WiFiTrustFusion"
                )

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
                    "M2.7 BOUNDARY"
                )

                appendLine(
                    "Session lifecycle: IMPLEMENTED"
                )

                appendLine(
                    "Repeated acquisition: IMPLEMENTED"
                )

                appendLine(
                    "Freshness engine: IMPLEMENTED"
                )

                appendLine(
                    "Persistent evidence logging: IMPLEMENTED"
                )

                appendLine(
                    "Clean-close marker: IMPLEMENTED"
                )

                appendLine(
                    "Room database: NOT IMPLEMENTED"
                )

                appendLine(
                    "WFAI: NOT IMPLEMENTED"
                )

                appendLine(
                    "Risk scoring: NOT IMPLEMENTED"
                )
            }

        updateButtons()
    }

    private fun resetLatestSummary() {

        latestRequestId =
            null

        latestResultCount =
            0

        latestFirstSeen =
            0

        latestAdvanced =
            0

        latestRepeated =
            0

        latestRegressed =
            0

        latestUntrackable =
            0

        latestSeenAfterRequest =
            0
    }

    private fun updateButtons() {

        if (
            !::startSessionButton.isInitialized
        ) {
            return
        }

        val sessionRunning =
            sessionController.isRunning()

        val scanRunning =
            scanCollector
                .isRequestInProgress()

        startSessionButton.isEnabled =
            !sessionRunning &&
                !scanRunning

        stopSessionButton.isEnabled =
            sessionRunning

        singleScanButton.isEnabled =
            !sessionRunning &&
                !scanRunning
    }


    private fun getAppVersionName():
        String {

        val packageInfo =
            packageManager.getPackageInfo(
                packageName,
                0
            )

        return packageInfo.versionName
            ?: "unknown"
    }

    private fun getAppVersionCode():
        Long {

        val packageInfo =
            packageManager.getPackageInfo(
                packageName,
                0
            )

        return if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.P
        ) {

            packageInfo.longVersionCode

        } else {

            @Suppress("DEPRECATION")
            packageInfo.versionCode
                .toLong()
        }
    }
    private fun elapsedRealtimeUs():
        Long {

        return SystemClock
            .elapsedRealtimeNanos() /
            1_000L
    }
}

