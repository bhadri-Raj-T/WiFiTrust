package com.bhadrirajt.wifitrustfusion

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bhadrirajt.wifitrustfusion.core.ProjectVersions
import com.bhadrirajt.wifitrustfusion.wifi.permission.PermissionCoordinator

class MainActivity : AppCompatActivity() {

    private lateinit var diagnosticText: TextView
    private lateinit var permissionCoordinator: PermissionCoordinator

    private val locationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
            renderDiagnostics()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        permissionCoordinator = PermissionCoordinator(this)

        diagnosticText = findViewById(R.id.textDiagnostics)

        findViewById<Button>(R.id.buttonRequestPermission)
            .setOnClickListener {
                requestCollectorPermissions()
            }

        findViewById<Button>(R.id.buttonLocationSettings)
            .setOnClickListener {
                openLocationSettings()
            }

        findViewById<Button>(R.id.buttonRefresh)
            .setOnClickListener {
                renderDiagnostics()
            }

        renderDiagnostics()
    }

    override fun onResume() {
        super.onResume()

        if (::permissionCoordinator.isInitialized) {
            renderDiagnostics()
        }
    }

    private fun requestCollectorPermissions() {
        val snapshot = permissionCoordinator.snapshot()

        if (snapshot.fineLocationGranted) {
            Toast.makeText(
                this,
                "Required location permission is already granted.",
                Toast.LENGTH_SHORT
            ).show()

            renderDiagnostics()
            return
        }

        locationPermissionLauncher.launch(
            permissionCoordinator.requiredRuntimePermissions()
        )
    }

    private fun openLocationSettings() {
        try {
            startActivity(
                Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            )
        } catch (_: Exception) {
            Toast.makeText(
                this,
                "Unable to open Location settings.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun renderDiagnostics() {
        val permission = permissionCoordinator.snapshot()

        val readinessLabel =
            if (permission.readyForWifiCollection) {
                "READY"
            } else {
                "NOT READY"
            }

        val diagnostics = buildString {
            appendLine("PROJECT")
            appendLine("WiFiTrustFusion")
            appendLine()

            appendLine("IMPLEMENTATION STATUS")
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

            appendLine("PERMISSION ARCHITECTURE")
            appendLine(
                "Fine location: " +
                        permission.fineLocationGranted
            )
            appendLine(
                "Coarse location: " +
                        permission.coarseLocationGranted
            )
            appendLine(
                "Location services: " +
                        permission.locationServicesEnabled
            )
            appendLine(
                "Collector prerequisites: $readinessLabel"
            )
            appendLine(
                "Reason: ${permission.readinessReason}"
            )
            appendLine()

            appendLine("SCORING MODULES")
            appendLine(
                "STAT: ${ProjectVersions.STAT_VERSION}"
            )
            appendLine(
                "TECH: ${ProjectVersions.TECH_VERSION}"
            )
            appendLine(
                "REVIEW: ${ProjectVersions.REVIEW_VERSION}"
            )
            appendLine(
                "CROWD: ${ProjectVersions.CROWD_VERSION}"
            )
            appendLine(
                "FUSION: ${ProjectVersions.FUSION_VERSION}"
            )
            appendLine()

            appendLine("M2.2 BOUNDARY")
            appendLine(
                "Permission architecture: IMPLEMENTED"
            )
            appendLine(
                "Wi-Fi acquisition: NOT IMPLEMENTED"
            )
            appendLine(
                "Observation model: NOT IMPLEMENTED"
            )
            appendLine(
                "Freshness engine: NOT IMPLEMENTED"
            )
            appendLine(
                "Database: NOT IMPLEMENTED"
            )
            appendLine(
                "Risk scoring: NOT IMPLEMENTED"
            )
        }

        diagnosticText.text = diagnostics
    }
}