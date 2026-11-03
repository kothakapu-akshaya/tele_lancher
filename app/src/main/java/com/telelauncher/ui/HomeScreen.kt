package com.telelauncher.ui

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppEntry(val label: String, val packageName: String)

@Composable
fun HomeScreen(launchApp: (String) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var apps by remember { mutableStateOf<List<AppEntry>>(emptyList()) }

    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) {
            val pm = context.packageManager
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 || pm.getLaunchIntentForPackage(it.packageName) != null }
                .mapNotNull { info ->
                    pm.getLaunchIntentForPackage(info.packageName)?.let {
                        AppEntry(pm.getApplicationLabel(info).toString(), info.packageName)
                    }
                }
                .sortedBy { it.label.lowercase() }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(32.dp)) {
        Text("Tele Launcher", color = Color.White, style = androidx.compose.material3.MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(24.dp))
        LazyVerticalGrid(columns = GridCells.Adaptive(140.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(apps) { app ->
                var focused by remember { mutableStateOf(false) }
                Card(
                    onClick = { launchApp(app.packageName) },
                    modifier = Modifier
                        .aspectRatio(1f)
                        .onFocusChanged { focused = it.isFocused }
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(app.label, textAlign = TextAlign.Center, color = if (focused) Color.Black else Color.White)
                    }
                }
            }
        }
    }
}