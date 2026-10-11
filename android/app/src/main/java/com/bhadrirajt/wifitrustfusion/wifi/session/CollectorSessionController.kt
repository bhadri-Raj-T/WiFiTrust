package com.bhadrirajt.wifitrustfusion.wifi.session

import android.os.Handler
import android.os.Looper
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID

class CollectorSessionController(
    private val listener: Listener
) {

    interface Listener {

        fun onSessionStarted(
            sessionId: String,
            intervalMs: Long
        )

        fun onScanDue(
            sessionId: String,
            intervalMs: Long
        )

        fun onSessionStopped(
            sessionId: String,
            completedRequestCount: Int,
            reason: String
        )

        fun onSessionStatus(
            message: String
        )
    }

    companion object {
        const val DEFAULT_DEV_INTERVAL_MS =
            35_000L
    }

    private val handler =
        Handler(
            Looper.getMainLooper()
        )

    private var state =
        CollectorSessionState.IDLE

    private var currentSessionId:
        String? = null

    private var requestedIntervalMs:
        Long =
        DEFAULT_DEV_INTERVAL_MS

    private var completedRequestCount =
        0

    private var nextScanScheduled =
        false

    private val nextScanRunnable =
        Runnable {

            nextScanScheduled = false

            val sessionId =
                currentSessionId
                    ?: return@Runnable

            if (
                state !=
                CollectorSessionState.RUNNING
            ) {
                return@Runnable
            }

            listener.onSessionStatus(
                "Scheduled scan is due."
            )

            listener.onScanDue(
                sessionId =
                    sessionId,
                intervalMs =
                    requestedIntervalMs
            )
        }

    fun startSession(
        deviceAlias: String,
        intervalMs: Long =
            DEFAULT_DEV_INTERVAL_MS
    ): Boolean {

        if (
            state ==
            CollectorSessionState.RUNNING
        ) {
            listener.onSessionStatus(
                "A collector session is already running."
            )

            return false
        }

        if (intervalMs <= 0L) {
            listener.onSessionStatus(
                "Invalid scan interval."
            )

            return false
        }

        handler.removeCallbacks(
            nextScanRunnable
        )

        nextScanScheduled =
            false

        completedRequestCount =
            0

        requestedIntervalMs =
            intervalMs

        currentSessionId =
            createSessionId(
                deviceAlias
            )

        state =
            CollectorSessionState.RUNNING

        val sessionId =
            currentSessionId!!

        listener.onSessionStarted(
            sessionId =
                sessionId,
            intervalMs =
                requestedIntervalMs
        )

        listener.onSessionStatus(
            "Session started. First scan requested immediately."
        )

        listener.onScanDue(
            sessionId =
                sessionId,
            intervalMs =
                requestedIntervalMs
        )

        return true
    }

    fun notifyScanCompleted() {

        if (
            state !=
            CollectorSessionState.RUNNING
        ) {
            return
        }

        completedRequestCount += 1

        scheduleNextScan()
    }

    fun stopSession(
        reason: String =
            "USER_STOP"
    ) {

        val sessionId =
            currentSessionId
                ?: return

        handler.removeCallbacks(
            nextScanRunnable
        )

        nextScanScheduled =
            false

        state =
            CollectorSessionState.STOPPED

        listener.onSessionStopped(
            sessionId =
                sessionId,
            completedRequestCount =
                completedRequestCount,
            reason =
                reason
        )

        listener.onSessionStatus(
            "Session stopped."
        )

        currentSessionId =
            null

        state =
            CollectorSessionState.IDLE
    }

    fun destroy() {

        handler.removeCallbacks(
            nextScanRunnable
        )

        nextScanScheduled =
            false

        currentSessionId =
            null

        state =
            CollectorSessionState.IDLE
    }

    fun isRunning(): Boolean {

        return state ==
            CollectorSessionState.RUNNING
    }

    fun getCurrentSessionId():
        String? {

        return currentSessionId
    }

    fun getCompletedRequestCount():
        Int {

        return completedRequestCount
    }

    fun getIntervalMs():
        Long {

        return requestedIntervalMs
    }

    private fun scheduleNextScan() {

        if (
            state !=
            CollectorSessionState.RUNNING
        ) {
            return
        }

        if (nextScanScheduled) {
            return
        }

        nextScanScheduled =
            true

        listener.onSessionStatus(
            "Next scan scheduled in " +
                "${requestedIntervalMs / 1_000L} seconds."
        )

        handler.postDelayed(
            nextScanRunnable,
            requestedIntervalMs
        )
    }

    private fun createSessionId(
        deviceAlias: String
    ): String {

        val safeAlias =
            deviceAlias
                .trim()
                .uppercase()
                .replace(
                    Regex("[^A-Z0-9]"),
                    ""
                )
                .take(12)
                .ifBlank {
                    "DEVICE"
                }

        val formatter =
            DateTimeFormatter
                .ofPattern(
                    "yyyyMMdd'T'HHmmss'Z'"
                )
                .withZone(
                    ZoneOffset.UTC
                )

        val timestamp =
            formatter.format(
                Instant.now()
            )

        val random =
            UUID.randomUUID()
                .toString()
                .take(4)
                .uppercase()

        return "${safeAlias}_${timestamp}_${random}"
    }
}
