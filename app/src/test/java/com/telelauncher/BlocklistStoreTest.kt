package com.telelauncher

import com.telelauncher.adblock.BlocklistStore
import com.telelauncher.adblock.HostsParser
import org.junit.Assert.*
import org.junit.Test

class BlocklistStoreTest {
    @Test
    fun parseHosts() {
        val t = "0.0.0.0 ads.example.com\n127.0.0.1 tracker.test #comment\n"
        val s = HostsParser.parse(t)
        assertTrue(s.contains("ads.example.com"))
        assertTrue(s.contains("tracker.test"))
    }

    @Test
    fun blockAllow() {
        val store = BlocklistStore()
        store.addSource(setOf("ads.example.com"))
        assertTrue(store.isBlocked("ads.example.com"))
        assertTrue(store.isBlocked("x.ads.example.com"))
        store.allow("ads.example.com")
        assertFalse(store.isBlocked("ads.example.com"))
    }
}