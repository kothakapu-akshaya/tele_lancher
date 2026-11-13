package com.telelauncher.ui

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.telelauncher.service.AdBlockVpnService

@Composable
fun AdBlockScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    var status by remember { mutableStateOf(if (AdBlockVpnService.running) "ON" else "OFF") }
    Column(Modifier.padding(40.dp)) {
        Text("AdBlock (DNS)", color = androidx.compose.ui.graphics.Color.White)
        Text("Status: $status")
        Text("Processed: ${AdBlockVpnService.processed}")
        Text("Blocked: ${AdBlockVpnService.blocked}")
        Text("Forwarded: ${AdBlockVpnService.forwarded}")
        Text("Upstream: ${AdBlockVpnService.upstream}")
        Button(onClick = {
            val intent = VpnService.prepare(ctx)
            if (intent != null) {
                (ctx as? Activity)?.startActivityForResult(intent, 1001)
            } else {
                ctx.startService(Intent(ctx, AdBlockVpnService::class.java))
                status = "ON"
            }
        }) { Text("Enable") }
        Button(onClick = {
            ctx.stopService(Intent(ctx, AdBlockVpnService::class.java))
            status = "OFF"
        }) { Text("Disable") }
        Button(onClick = onBack) { Text("Back") }
    }
}