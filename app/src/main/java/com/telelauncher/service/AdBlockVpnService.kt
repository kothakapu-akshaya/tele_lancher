package com.telelauncher.service

import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import com.telelauncher.data.AdBlockDns
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class AdBlockVpnService : VpnService() {
    companion object {
        @Volatile var running = false
        @Volatile var processed = 0L
        @Volatile var blocked = 0L
        @Volatile var forwarded = 0L
        val store = com.telelauncher.adblock.BlocklistStore()
        @Volatile var blocklist: Set<String> = emptySet()
        var upstream = "8.8.8.8"
    }

    private var tun: ParcelFileDescriptor? = null
    private var thread: Thread? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (running) return START_STICKY
        running = true
        thread = Thread { runLoop() }.also { it.start() }
        return START_STICKY
    }

    private fun runLoop() {
        val builder = Builder()
            .addAddress("10.0.0.2", 32)
            .addDnsServer("10.0.0.1")
            .addRoute("10.0.0.1", 32)
            .setSession("TeleLauncher AdBlock")
        tun = builder.establish() ?: run { running = false; return }
        val input = FileInputStream(tun!!.fileDescriptor)
        val output = FileOutputStream(tun!!.fileDescriptor)
        val buf = ByteArray(32767)
        while (running) {
            val n = input.read(buf)
            if (n <= 0) continue
            // Minimal: treat payload as DNS if UDP; parse domain
            try {
                val domain = AdBlockDns.parseQueryDomain(buf.copyOf(n))
                if (domain != null) {
                    processed++
                    if (store.isBlocked(domain)) {
                        blocked++
                        output.write(AdBlockDns.buildNxdomain(buf.copyOf(n)))
                    } else {
                        forwarded++
                        val sock = DatagramSocket()
                        protect(sock)
                        sock.soTimeout = 5000
                        val packet = DatagramPacket(buf, n, InetAddress.getByName(upstream), 53)
                        sock.send(packet)
                        val rbuf = ByteArray(32767)
                        val rp = DatagramPacket(rbuf, rbuf.size)
                        sock.receive(rp)
                        output.write(rbuf, 0, rp.length)
                        sock.close()
                    }
                }
            } catch (e: Exception) {
                // ignore malformed
            }
        }
    }

    override fun onDestroy() {
        running = false
        thread?.interrupt()
        tun?.close()
        super.onDestroy()
    }
}