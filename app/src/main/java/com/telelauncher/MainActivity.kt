package com.telelauncher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.key
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.telelauncher.ui.HomeScreen
import com.telelauncher.ui.TeleLauncherTheme

class MainActivity : ComponentActivity() {
    private val launchKey = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        setContent {
            TeleLauncherTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = androidx.compose.ui.graphics.Color(0xFF0B0E14)) {
                    key(launchKey.intValue) {
                        HomeScreen(
                            startInAppLauncher = intent.categories?.contains(android.content.Intent.CATEGORY_HOME) == true,
                            launchApp = { pkg ->
                                packageManager.getLaunchIntentForPackage(pkg)?.let { startActivity(it) }
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        launchKey.intValue++
    }
}