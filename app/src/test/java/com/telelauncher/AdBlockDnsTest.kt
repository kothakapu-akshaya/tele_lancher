package com.telelauncher

import com.telelauncher.data.AdBlockDns
import org.junit.Assert.*
import org.junit.Test

class AdBlockDnsTest {
    @Test
    fun normalizeAndSubdomain() {
        val bl = setOf("ads.example.com")
        assertTrue(AdBlockDns.isBlocked("ads.example.com", bl))
        assertTrue(AdBlockDns.isBlocked("SUB.Ads.Example.Com.", bl))
        assertFalse(AdBlockDns.isBlocked("example.com", bl))
    }

    @Test
    fun parseDomain() {
        // craft minimal DNS query for "a.b"
        val bytes = byteArrayOf(
            0,1, // id
            0,0, // flags
            0,1,0,0,0,0,0,0, // counts
            1,'a'.code.toByte(), 1,'b'.code.toByte(), 0,
            0,1, 0,1
        )
        assertEquals("a.b", AdBlockDns.parseQueryDomain(bytes))
    }

    @Test
    fun nxdomain() {
        val req = ByteArray(12) { 0 }
        val r = AdBlockDns.buildNxdomain(req)
        assertEquals(0x81.toByte(), r[2])
        assertEquals(0x83.toByte(), r[3])
    }
}