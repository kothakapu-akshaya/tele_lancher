package com.telelauncher.data

object AdBlockDns {
    fun normalize(domain: String): String = domain.trim().trimEnd('.').lowercase()

    fun isBlocked(domain: String, blocklist: Set<String>): Boolean {
        val d = normalize(domain)
        if (d in blocklist) return true
        // subdomain match
        var cur = d
        while (true) {
            val idx = cur.indexOf('.')
            if (idx < 0) break
            cur = cur.substring(idx + 1)
            if (cur in blocklist) return true
        }
        return false
    }

    // Minimal DNS question parser: returns first QNAME or null
    fun parseQueryDomain(packet: ByteArray): String? {
        if (packet.size < 12) return null
        var i = 12
        val labels = mutableListOf<String>()
        while (i < packet.size) {
            val len = packet[i].toInt() and 0xFF
            if (len == 0) { i++; break }
            if (i + 1 + len > packet.size) return null
            labels.add(String(packet, i + 1, len, Charsets.US_ASCII))
            i += 1 + len
        }
        return labels.joinToString(".")
    }

    // Build a DNS response with RCODE=NXDOMAIN (3) for blocked domain, copying ID
    fun buildNxdomain(request: ByteArray): ByteArray {
        if (request.size < 12) return request
        val resp = request.copyOf()
        // flags: standard response, authoritative, NXDOMAIN
        resp[2] = 0x81.toByte()
        resp[3] = 0x83.toByte()
        // qdcount keep; ancount=0, nscount=0, arcount=0
        resp[6] = 0; resp[7] = 0
        resp[8] = 0; resp[9] = 0
        resp[10] = 0; resp[11] = 0
        return resp
    }
}