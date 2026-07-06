package com.rms.customs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Placeholder shared entry point proving Compose Multiplatform renders
 * identically across targets. Not wired into the existing Android UI yet —
 * that migration happens in later phases.
 */
@Composable
fun App() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "RMS Customs Tracker",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "iOS skeleton — Compose Multiplatform",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
