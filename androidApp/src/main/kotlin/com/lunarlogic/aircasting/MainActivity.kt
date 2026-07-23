package com.lunarlogic.aircasting

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview

private val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            // Every permission result bumps this; App re-runs its load with the new grant.
            var refreshSignal by remember { mutableIntStateOf(0) }
            val launcher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { refreshSignal++ }

            // Ask once on first launch; AndroidLocationProvider needs a runtime grant.
            LaunchedEffect(Unit) { launcher.launch(LOCATION_PERMISSIONS) }

            App(
                onRequestLocation = { launcher.launch(LOCATION_PERMISSIONS) },
                refreshSignal = refreshSignal,
            )
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}