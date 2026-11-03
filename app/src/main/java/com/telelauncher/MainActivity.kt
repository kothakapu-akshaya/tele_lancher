package com.telelauncher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.telelauncher.ui.HomeScreen
import com.telelauncher.ui.TeleLauncherTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TeleLauncherTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = androidx.compose.ui.graphics.Color(0xFF0B0E14)) {
                    HomeScreen(launchApp = { pkg ->
                        packageManager.getLaunchIntentForPackage(pkg)?.let { startActivity(it) }
                    })
                }
            }
        }
    }
}