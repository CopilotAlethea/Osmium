package org.dazai.osmium.util

import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.util.zip.ZipFile
import kotlin.concurrent.thread

class EpubServer(private val epubPath: String) {
    private var serverSocket: ServerSocket? = null
    private var port: Int = 0
    private var running = false

    fun start(): Int {
        serverSocket = ServerSocket(0)
        port = serverSocket!!.localPort
        running = true
        thread(isDaemon = true) {
            while (running) {
                try {
                    val client = serverSocket!!.accept()
                    thread(isDaemon = true) { handleClient(client) }
                } catch (e: Exception) {
                    if (running) e.printStackTrace()
                }
            }
        }
        return port
    }

    fun stop() {
        running = false
        serverSocket?.close()
    }

    private fun handleClient(client: Socket) {
        try {
            val input = client.getInputStream().bufferedReader()
            val line = input.readLine() ?: return
            val parts = line.split(" ")
            if (parts.size < 2) return
            
            var path = parts[1]
            if (path.startsWith("/")) path = path.drop(1)
            // Remove query parameters
            path = path.substringBefore("?")

            ZipFile(File(epubPath)).use { zip ->
                val entry = zip.getEntry(path)
                val output = client.getOutputStream()
                if (entry != null) {
                    output.write("HTTP/1.1 200 OK\r\n".toByteArray())
                    output.write("Content-Type: ${getContentType(path)}\r\n".toByteArray())
                    output.write("Access-Control-Allow-Origin: *\r\n".toByteArray())
                    output.write("Content-Length: ${entry.size}\r\n".toByteArray())
                    output.write("\r\n".toByteArray())
                    zip.getInputStream(entry).use { it.copyTo(output) }
                } else {
                    output.write("HTTP/1.1 404 Not Found\r\n".toByteArray())
                    output.write("\r\n".toByteArray())
                }
                output.flush()
            }
        } catch (e: Exception) {
            // e.printStackTrace()
        } finally {
            client.close()
        }
    }

    private fun getContentType(path: String): String {
        return when (path.substringAfterLast(".", "").lowercase()) {
            "html", "xhtml" -> "application/xhtml+xml"
            "css" -> "text/css"
            "js" -> "application/javascript"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "svg" -> "image/svg+xml"
            "otf" -> "font/otf"
            "ttf" -> "font/ttf"
            "woff" -> "font/woff"
            "woff2" -> "font/woff2"
            else -> "application/octet-stream"
        }
    }
}

