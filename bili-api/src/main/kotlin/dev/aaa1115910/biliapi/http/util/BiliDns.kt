package dev.aaa1115910.biliapi.http.util

import okhttp3.Dns
import java.net.Inet4Address
import java.net.InetAddress

object BiliDns : Dns {
    var ipv4Only: Boolean = false

    override fun lookup(hostname: String): List<InetAddress> {
        val addresses = Dns.SYSTEM.lookup(hostname)
        if (!ipv4Only) return addresses
        val ipv4Addresses = addresses.filter { it is Inet4Address }
        return ipv4Addresses.ifEmpty { addresses }
    }
}
