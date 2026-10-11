package com.bhadrirajt.wifitrustfusion

import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bhadrirajt.wifitrustfusion.core.ProjectVersions

class MainActivity : AppCompatActivity() {

    private lateinit var diagnosticText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        diagnosticText = findViewById(R.id.textDiagnostics)

        findViewById<Button>(R.id.buttonRefresh).setOnClickListener {
            renderDiagnostics()
        }

        renderDiagnostics()
    }

    private fun renderDiagnostics() {
        val diagnostics = buildString {
            appendLine("PROJECT")
            appendLine("WiFiTrustFusion")
            appendLine()

            appendLine("IMPLEMENTATION STATUS")
            appendLine("Milestone: ${ProjectVersions.MILESTONE}")
            appendLine("Collector: ${ProjectVersions.COLLECTOR_VERSION}")
            appendLine("Protocol: ${ProjectVersions.PROTOCOL_VERSION}")
            appendLine("Observation schema: ${ProjectVersions.OBS_SCHEMA_VERSION}")
            appendLine()

            appendLine("SCORING MODULES")
            appendLine("STAT: ${ProjectVersions.STAT_VERSION}")
            appendLine("TECH: ${ProjectVersions.TECH_VERSION}")
            appendLine("REVIEW: ${ProjectVersions.REVIEW_VERSION}")
            appendLine("CROWD: ${ProjectVersions.CROWD_VERSION}")
            appendLine("FUSION: ${ProjectVersions.FUSION_VERSION}")
            appendLine()

            appendLine("DEVICE ENVIRONMENT")
            appendLine("Manufacturer: ${Build.MANUFACTURER}")
            appendLine("Model: ${Build.MODEL}")
            appendLine("Android release: ${Build.VERSION.RELEASE}")
            appendLine("Runtime API: ${Build.VERSION.SDK_INT}")
            appendLine()

            appendLine("M1 BOUNDARY")
            appendLine("Wi-Fi acquisition: NOT IMPLEMENTED")
            appendLine("Database: NOT IMPLEMENTED")
            appendLine("WFAI: NOT IMPLEMENTED")
            appendLine("Technical tests: NOT IMPLEMENTED")
            appendLine("Review trust: NOT IMPLEMENTED")
            appendLine("Final risk fusion: NOT IMPLEMENTED")
        }

        diagnosticText.text = diagnostics
    }
}