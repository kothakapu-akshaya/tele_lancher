package com.telelauncher.adblock

class BlocklistStore {
    private val blocked = mutableSetOf<String>()
    private val allowed = mutableSetOf<String>()
    private val customBlocked = mutableSetOf<String>()
    private val customAllowed = mutableSetOf<String>()

    fun addSource(hosts: Set<String>) { blocked.addAll(hosts) }
    fun allow(domain: String) { allowed.add(domain.lowercase()) }
    fun block(domain: String) { customBlocked.add(domain.lowercase()) }
    fun unblock(domain: String) { customBlocked.remove(domain.lowercase()); blocked.remove(domain.lowercase()) }

    fun isBlocked(domain: String): Boolean {
        val d = domain.lowercase().trimEnd('.')
        if (d in allowed || d in customAllowed) return false
        if (d in customBlocked) return true
        if (d in blocked) return true
        // subdomain
        var cur = d
        while (cur.contains('.')) {
            cur = cur.substringAfter('.')
            if (cur in allowed || cur in customAllowed) return false
            if (cur in blocked || cur in customBlocked) return true
        }
        return false
    }

    fun merge() = blocked.size + customBlocked.size
    fun clear() { blocked.clear(); allowed.clear(); customBlocked.clear(); customAllowed.clear() }
    fun allBlocked(): Set<String> = (blocked + customBlocked) - allowed - customAllowed
}