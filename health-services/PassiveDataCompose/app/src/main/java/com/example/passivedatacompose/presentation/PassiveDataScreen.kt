/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.passivedatacompose.presentation

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Text
import com.example.passivedatacompose.PERMISSION
import com.example.passivedatacompose.theme.PassiveDataTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.delay

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PassiveDataScreen(
    hrValue: Double,
    hrEnabled: Boolean,
    onEnableClick: (Boolean) -> Unit,
    permissionState: PermissionState,
    isRecording: Boolean,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit
) {
    val context = LocalContext.current
    val vibrator = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    val recordAudioPermissionState = rememberPermissionState(
        permission = Manifest.permission.RECORD_AUDIO,
        onPermissionResult = { granted ->
            if (granted) {
                onStartRecording()
                // Haptics for START: 1 short buzz.
                val startVibration =
                    VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE)
                vibrator.vibrate(startVibration)
            }
        }
    )

    // Timer effect for the 2-minute recording duration
    LaunchedEffect(isRecording) {
        if (isRecording) {
            delay(120_000) // 2 minutes
            onStopRecording()

            // Haptics for STOP: 2 short buzzes.
            val stopVibration = VibrationEffect.createWaveform(longArrayOf(0, 150, 100, 150), -1)
            vibrator.vibrate(stopVibration)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        HeartRateToggle(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            checked = hrEnabled,
            onCheckedChange = onEnableClick,
            permissionState = permissionState
        )
        HeartRateCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            heartRate = hrValue
        )

        // New Button for manual recording
        Button(
            onClick = {
                if (isRecording) {
                    onStopRecording()
                    // Haptics for STOP: 2 short buzzes.
                    val stopVibration = VibrationEffect.createWaveform(longArrayOf(0, 150, 100, 150), -1)
                    vibrator.vibrate(stopVibration)
                } else {
                    recordAudioPermissionState.launchPermissionRequest()
                }
            },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text(if (isRecording) "Stop" else "Manual Record (2 min)")
        }
    }
}

@ExperimentalPermissionsApi
@Preview(
    device = Devices.WEAR_OS_SMALL_ROUND,
    showBackground = false,
    showSystemUi = true
)
@Composable
fun PassiveDataScreenPreview() {
    val permissionState = object : PermissionState {
        override val permission = PERMISSION
        override val status: PermissionStatus = PermissionStatus.Granted
        override fun launchPermissionRequest() {}
    }
    PassiveDataTheme {
        PassiveDataScreen(
            hrValue = 65.6,
            hrEnabled = true,
            onEnableClick = { },
            permissionState = permissionState,
            isRecording = false,
            onStartRecording = {},
            onStopRecording = {}
        )
    }
}
