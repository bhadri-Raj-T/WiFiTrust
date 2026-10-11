package com.bhadrirajt.wifitrustfusion.wifi.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat

class PermissionCoordinator(
    private val context: Context
) {

    data class PermissionSnapshot(
        val fineLocationGranted: Boolean,
        val coarseLocationGranted: Boolean,
        val locationServicesEnabled: Boolean,
        val readyForWifiCollection: Boolean,
        val readinessReason: String
    )

    fun requiredRuntimePermissions(): Array<String> {
        return arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    fun snapshot(): PermissionSnapshot {
        val fineGranted = hasPermission(
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        val coarseGranted = hasPermission(
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        val locationEnabled = isLocationServicesEnabled()

        val ready = fineGranted && locationEnabled

        val reason = when {
            !fineGranted && coarseGranted ->
                "Precise location permission is required for Wi-Fi scan collection."

            !fineGranted ->
                "Location permission has not been granted."

            !locationEnabled ->
                "Device location services are disabled."

            else ->
                "Permission prerequisites satisfied."
        }

        return PermissionSnapshot(
            fineLocationGranted = fineGranted,
            coarseLocationGranted = coarseGranted,
            locationServicesEnabled = locationEnabled,
            readyForWifiCollection = ready,
            readinessReason = reason
        )
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun isLocationServicesEnabled(): Boolean {
        val locationManager = context.getSystemService(
            Context.LOCATION_SERVICE
        ) as LocationManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            locationManager.isLocationEnabled
        } else {
            val gpsEnabled = try {
                locationManager.isProviderEnabled(
                    LocationManager.GPS_PROVIDER
                )
            } catch (_: Exception) {
                false
            }

            val networkEnabled = try {
                locationManager.isProviderEnabled(
                    LocationManager.NETWORK_PROVIDER
                )
            } catch (_: Exception) {
                false
            }

            gpsEnabled || networkEnabled
        }
    }
}