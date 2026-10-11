package com.bhadrirajt.wifitrustfusion.wifi.scanner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.bhadrirajt.wifitrustfusion.wifi.model.RawWifiScanObservation
import com.bhadrirajt.wifitrustfusion.wifi.model.ScanCompletionReason
import com.bhadrirajt.wifitrustfusion.wifi.model.ScanRequestRecord
import com.bhadrirajt.wifitrustfusion.wifi.permission.PermissionCoordinator
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong

class WifiScanCollector(
    context: Context,
    private val permissionCoordinator: PermissionCoordinator,
    private val listener: Listener
) {

    interface Listener {
        fun onScanCompleted(batch: WifiScanBatch)

        fun onCollectorStatus(message: String)
    }

    private data class PendingRequest(
        val sessionId: String,
        val requestId: String,
        val requestUtcIso: String,
        val requestElapsedUs: Long,
        val requestedIntervalMs: Long
    )

    companion object {
        private const val SCAN_TIMEOUT_MS = 15_000L
    }

    private val appContext = context.applicationContext

    private val wifiManager =
        appContext.getSystemService(
            Context.WIFI_SERVICE
        ) as WifiManager

    private val handler =
        Handler(Looper.getMainLooper())

    private val requestCounter =
        AtomicLong(0L)

    private var receiverRegistered = false

    private var pendingRequest: PendingRequest? = null

    private val timeoutRunnable = Runnable {
        val pending = pendingRequest ?: return@Runnable

        pendingRequest = null

        listener.onScanCompleted(
            WifiScanBatch(
                request = buildCompletedRequest(
                    pending = pending,
                    startScanReturned = true,
                    broadcastReceived = false,
                    resultsUpdated = null,
                    resultCount = 0,
                    completionReason =
                        ScanCompletionReason.SCAN_TIMEOUT,
                    failureDetail =
                        "No scan-results broadcast received within timeout."
                ),
                observations = emptyList()
            )
        )

        listener.onCollectorStatus(
            "Scan request ${pending.requestId} timed out."
        )
    }

    private val scanReceiver =
        object : BroadcastReceiver() {

            override fun onReceive(
                context: Context?,
                intent: Intent?
            ) {
                if (
                    intent?.action !=
                    WifiManager.SCAN_RESULTS_AVAILABLE_ACTION
                ) {
                    return
                }

                val pending =
                    pendingRequest ?: return

                handler.removeCallbacks(
                    timeoutRunnable
                )

                val resultsUpdated =
                    intent.getBooleanExtra(
                        WifiManager.EXTRA_RESULTS_UPDATED,
                        false
                    )

                val observations =
                    try {
                        @Suppress("MissingPermission")
                        wifiManager.scanResults.map { result ->
                            RawWifiScanObservation(
                                ssid =
                                    result.SSID
                                        ?.takeIf {
                                            it.isNotBlank()
                                        },

                                bssid =
                                    result.BSSID
                                        ?.takeIf {
                                            it.isNotBlank()
                                        },

                                rssiDbm =
                                    result.level,

                                frequencyMhz =
                                    result.frequency,

                                channelWidth =
                                    result.channelWidth,

                                capabilities =
                                    result.capabilities
                                        ?: "",

                                scanTimestampUs =
                                    result.timestamp
                                        .takeIf {
                                            it > 0L
                                        }
                            )
                        }
                    } catch (
                        exception: SecurityException
                    ) {
                        pendingRequest = null

                        listener.onScanCompleted(
                            WifiScanBatch(
                                request =
                                    buildCompletedRequest(
                                        pending = pending,
                                        startScanReturned = true,
                                        broadcastReceived = true,
                                        resultsUpdated =
                                            resultsUpdated,
                                        resultCount = 0,
                                        completionReason =
                                            ScanCompletionReason
                                                .SECURITY_EXCEPTION,
                                        failureDetail =
                                            exception.message
                                    ),
                                observations =
                                    emptyList()
                            )
                        )

                        listener.onCollectorStatus(
                            "SecurityException while reading scan results."
                        )

                        return
                    }

                pendingRequest = null

                val completionReason =
                    if (resultsUpdated) {
                        ScanCompletionReason
                            .BROADCAST_RESULTS_UPDATED
                    } else {
                        ScanCompletionReason
                            .BROADCAST_RESULTS_NOT_UPDATED
                    }

                listener.onScanCompleted(
                    WifiScanBatch(
                        request =
                            buildCompletedRequest(
                                pending = pending,
                                startScanReturned = true,
                                broadcastReceived = true,
                                resultsUpdated =
                                    resultsUpdated,
                                resultCount =
                                    observations.size,
                                completionReason =
                                    completionReason,
                                failureDetail = null
                            ),
                        observations =
                            observations
                    )
                )

                listener.onCollectorStatus(
                    "Scan request ${pending.requestId} completed."
                )
            }
        }

    fun register() {
        if (receiverRegistered) {
            return
        }

        val filter =
            IntentFilter(
                WifiManager.SCAN_RESULTS_AVAILABLE_ACTION
            )

        ContextCompat.registerReceiver(
            appContext,
            scanReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )

        receiverRegistered = true

        listener.onCollectorStatus(
            "Wi-Fi scan receiver registered."
        )
    }

    fun unregister() {
        handler.removeCallbacks(
            timeoutRunnable
        )

        val pending =
            pendingRequest

        if (pending != null) {
            pendingRequest = null

            listener.onScanCompleted(
                WifiScanBatch(
                    request =
                        buildCompletedRequest(
                            pending = pending,
                            startScanReturned = true,
                            broadcastReceived = false,
                            resultsUpdated = null,
                            resultCount = 0,
                            completionReason =
                                ScanCompletionReason
                                    .ACTIVITY_LEFT_FOREGROUND,
                            failureDetail =
                                "Collector left foreground before request completed."
                        ),
                    observations =
                        emptyList()
                )
            )
        }

        if (receiverRegistered) {
            try {
                appContext.unregisterReceiver(
                    scanReceiver
                )
            } catch (_: Exception) {
            }

            receiverRegistered = false
        }
    }

    fun isRequestInProgress(): Boolean {
        return pendingRequest != null
    }

    fun isWifiEnabled(): Boolean {
        return wifiManager.isWifiEnabled
    }

    fun requestSingleScan(
        sessionId: String,
        requestedIntervalMs: Long = 0L
    ) {
        if (pendingRequest != null) {
            listener.onCollectorStatus(
                "A scan request is already in progress."
            )
            return
        }

        val requestNumber =
            requestCounter.incrementAndGet()

        val pending =
            PendingRequest(
                sessionId = sessionId,

                requestId =
                    "REQ_%06d".format(
                        requestNumber
                    ),

                requestUtcIso =
                    Instant.now().toString(),

                requestElapsedUs =
                    elapsedRealtimeUs(),

                requestedIntervalMs =
                    requestedIntervalMs
            )

        val permission =
            permissionCoordinator.snapshot()

        if (!permission.fineLocationGranted) {
            completeBlockedRequest(
                pending =
                    pending,
                reason =
                    ScanCompletionReason
                        .PERMISSION_MISSING,
                detail =
                    "Fine location permission is missing."
            )
            return
        }

        if (!permission.locationServicesEnabled) {
            completeBlockedRequest(
                pending =
                    pending,
                reason =
                    ScanCompletionReason
                        .LOCATION_DISABLED,
                detail =
                    "Device location services are disabled."
            )
            return
        }

        if (!wifiManager.isWifiEnabled) {
            completeBlockedRequest(
                pending =
                    pending,
                reason =
                    ScanCompletionReason
                        .WIFI_DISABLED,
                detail =
                    "Wi-Fi is disabled."
            )
            return
        }

        pendingRequest = pending

        listener.onCollectorStatus(
            "Starting ${pending.requestId}..."
        )

        val accepted =
            try {
                @Suppress("DEPRECATION")
                wifiManager.startScan()
            } catch (
                exception: SecurityException
            ) {
                pendingRequest = null

                listener.onScanCompleted(
                    WifiScanBatch(
                        request =
                            buildCompletedRequest(
                                pending = pending,
                                startScanReturned = null,
                                broadcastReceived = false,
                                resultsUpdated = null,
                                resultCount = 0,
                                completionReason =
                                    ScanCompletionReason
                                        .SECURITY_EXCEPTION,
                                failureDetail =
                                    exception.message
                            ),
                        observations =
                            emptyList()
                    )
                )

                listener.onCollectorStatus(
                    "SecurityException from startScan()."
                )

                return
            } catch (
                exception: Exception
            ) {
                pendingRequest = null

                listener.onScanCompleted(
                    WifiScanBatch(
                        request =
                            buildCompletedRequest(
                                pending = pending,
                                startScanReturned = null,
                                broadcastReceived = false,
                                resultsUpdated = null,
                                resultCount = 0,
                                completionReason =
                                    ScanCompletionReason
                                        .COLLECTOR_ERROR,
                                failureDetail =
                                    exception.message
                            ),
                        observations =
                            emptyList()
                    )
                )

                listener.onCollectorStatus(
                    "Collector error while starting scan."
                )

                return
            }

        if (!accepted) {
            pendingRequest = null

            listener.onScanCompleted(
                WifiScanBatch(
                    request =
                        buildCompletedRequest(
                            pending = pending,
                            startScanReturned = false,
                            broadcastReceived = false,
                            resultsUpdated = null,
                            resultCount = 0,
                            completionReason =
                                ScanCompletionReason
                                    .START_SCAN_RETURNED_FALSE,
                            failureDetail =
                                "WifiManager.startScan() returned false."
                        ),
                    observations =
                        emptyList()
                )
            )

            listener.onCollectorStatus(
                "${pending.requestId}: startScan() returned false."
            )

            return
        }

        handler.postDelayed(
            timeoutRunnable,
            SCAN_TIMEOUT_MS
        )
    }

    private fun completeBlockedRequest(
        pending: PendingRequest,
        reason: ScanCompletionReason,
        detail: String
    ) {
        listener.onScanCompleted(
            WifiScanBatch(
                request =
                    buildCompletedRequest(
                        pending = pending,
                        startScanReturned = null,
                        broadcastReceived = false,
                        resultsUpdated = null,
                        resultCount = 0,
                        completionReason = reason,
                        failureDetail = detail
                    ),
                observations =
                    emptyList()
            )
        )

        listener.onCollectorStatus(
            "${pending.requestId} blocked: $detail"
        )
    }

    private fun buildCompletedRequest(
        pending: PendingRequest,
        startScanReturned: Boolean?,
        broadcastReceived: Boolean,
        resultsUpdated: Boolean?,
        resultCount: Int,
        completionReason: ScanCompletionReason,
        failureDetail: String?
    ): ScanRequestRecord {

        return ScanRequestRecord(
            sessionId =
                pending.sessionId,

            requestId =
                pending.requestId,

            requestUtcIso =
                pending.requestUtcIso,

            requestElapsedUs =
                pending.requestElapsedUs,

            requestedIntervalMs =
                pending.requestedIntervalMs,

            startScanReturned =
                startScanReturned,

            broadcastReceived =
                broadcastReceived,

            resultsUpdated =
                resultsUpdated,

            resultCount =
                resultCount,

            completionReason =
                completionReason,

            completedUtcIso =
                Instant.now().toString(),

            completedElapsedUs =
                elapsedRealtimeUs(),

            failureDetail =
                failureDetail
        )
    }

    private fun elapsedRealtimeUs(): Long {
        return SystemClock.elapsedRealtimeNanos() /
            1_000L
    }
}
