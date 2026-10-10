package com.ahmedbecett.filemaster

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.IOException
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Collections

/**
 * Temporary, read-only HTTP sharing of explicitly selected files over the current local network.
 * The server binds to all interfaces and must be stopped by the user after transfer.
 */
class LocalFileShareServer(
    private val context: Context,
    private val sharedFiles: List<DocumentFile>
) : AutoCloseable {
    private val server = ServerSocket(0, 8, InetAddress.getByName("0.0.0.0"))
    @Volatile private var running = true
    val url: String = "http://" + findLocalIpv4() + ":" + server.localPort + "/"

    fun serve() {
        while (running && !server.isClosed) {
            try {
                val client = server.accept()
                Thread({ handleClient(client) }, "FileMaster-LAN-client").start()
            } catch (_: IOException) {
                if (running) continue else break
            }
        }
    }

    private fun handleClient(socket: Socket) {
        socket.use { client ->
            client.soTimeout = 15_000
            val input = BufferedInputStream(client.getInputStream())
            val output = BufferedOutputStream(client.getOutputStream())
            val requestLine = input.readLineUtf8() ?: return
            val pieces = requestLine.split(' ')
            if (pieces.size < 2 || pieces[0] != "GET") {
                respond(output, "405 Method Not Allowed", "text/plain; charset=utf-8", "Only GET is supported.")
                return
            }
            while (true) {
                val header = input.readLineUtf8() ?: break
                if (header.isEmpty()) break
            }
            val path = pieces[1].substringBefore('?')
            if (path == "/" || path == "/index.html") {
                val html = buildString {
                    append("<!doctype html><html><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><title>FileMaster Share</title></head><body style=\"font:16px sans-serif;max-width:720px;margin:30px auto;padding:0 16px\"><h2>FileMaster — Local file sharing</h2><p>Tap a file to download it. This page is available only while the phone's sharing server is running.</p><ul>")
                    sharedFiles.forEachIndexed { index, file ->
                        append("<li><a href=\"/file/").append(index).append("\">")
                        append(escapeHtml(file.name ?: "file-").replace("\"", "&quot;"))
                        append("</a> — ").append(file.length().coerceAtLeast(0L)).append(" bytes</li>")
                    }
                    append("</ul></body></html>")
                }
                respond(output, "200 OK", "text/html; charset=utf-8", html)
                return
            }
            val index = path.removePrefix("/file/").toIntOrNull()
            if (!path.startsWith("/file/") || index == null || index !in sharedFiles.indices) {
                respond(output, "404 Not Found", "text/plain; charset=utf-8", "Not found.")
                return
            }
            val file = sharedFiles[index]
            if (file.isDirectory) {
                respond(output, "400 Bad Request", "text/plain; charset=utf-8", "Folders are not shared directly. Select files instead.")
                return
            }
            val name = file.name ?: "download.bin"
            output.write(("HTTP/1.1 200 OK\r\nContent-Type: application/octet-stream\r\nContent-Disposition: attachment; filename*=UTF-8''" +
                URLEncoder.encode(name, StandardCharsets.UTF_8.name()).replace("+", "%20") +
                "\r\nConnection: close\r\nX-Content-Type-Options: nosniff\r\n\r\n").toByteArray(StandardCharsets.UTF_8))
            output.flush()
            context.contentResolver.openInputStream(file.uri)?.use { source ->
                val buffer = ByteArray(32 * 1024)
                while (running) {
                    val count = source.read(buffer)
                    if (count < 0) break
                    output.write(buffer, 0, count)
                }
            }
            output.flush()
        }
    }

    private fun respond(output: BufferedOutputStream, status: String, contentType: String, body: String) {
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        output.write(("HTTP/1.1 " + status + "\r\nContent-Type: " + contentType +
            "\r\nContent-Length: " + bytes.size + "\r\nConnection: close\r\nX-Content-Type-Options: nosniff\r\n\r\n").toByteArray(StandardCharsets.UTF_8))
        output.write(bytes)
        output.flush()
    }

    override fun close() {
        running = false
        try { server.close() } catch (_: IOException) { }
    }

    private fun findLocalIpv4(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (!networkInterface.isUp || networkInterface.isLoopback) continue
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (address is Inet4Address && !address.isLoopbackAddress && !address.isLinkLocalAddress) {
                        return address.hostAddress ?: continue
                    }
                }
            }
        } catch (_: Exception) { }
        return "127.0.0.1"
    }

    private fun BufferedInputStream.readLineUtf8(): String? {
        val bytes = java.io.ByteArrayOutputStream()
        while (bytes.size() < 8192) {
            val value = read()
            if (value < 0) return if (bytes.size() == 0) null else bytes.toString("UTF-8")
            if (value == '\n'.code) break
            if (value != '\r'.code) bytes.write(value)
        }
        return bytes.toString("UTF-8")
    }

    private fun escapeHtml(value: String): String = value
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}
