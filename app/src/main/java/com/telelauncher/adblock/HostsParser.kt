package com.telelauncher.adblock

object HostsParser {
    fun parse(text: String): Set<String> {
        val out = mutableSetOf<String>()
        for (line in text.lines()) {
            val t = line.trim()
            if (t.isEmpty() || t.startsWith("#")) continue
            val parts = t.split(Regex("\\s+"))
            if (parts.size < 2) continue
            val host = parts[1].trim().lowercase().trimEnd('.')
            if (host.isNotEmpty() && host != "localhost") out.add(host)
        }
        return out
    }
}