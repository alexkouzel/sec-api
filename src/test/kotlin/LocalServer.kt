package com.alexkouzel.sec

import com.sun.net.httpserver.Headers
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.io.ByteArrayOutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArrayList
import java.util.zip.GZIPOutputStream

// HTTP server on localhost for client tests. Serves queued responses in order (the last one
// repeats once the queue runs dry) and records every request.
internal class LocalServer : AutoCloseable {
    private val address = InetSocketAddress(InetAddress.getLoopbackAddress(), 0)
    private val server = HttpServer.create(address, 0)
    private val queue = ConcurrentLinkedQueue<Response>()
    private val received = CopyOnWriteArrayList<Request>()

    @Volatile
    private var last = Response.ok("")

    val requests: List<Request> get() = received.toList()

    init {
        server.createContext("/", ::handle)
        server.start()
    }

    fun enqueue(vararg responses: Response): LocalServer = apply { queue += responses }

    fun url(path: String): String = "http://127.0.0.1:${server.address.port}$path"

    override fun close() {
        server.stop(0)
    }

    private fun handle(exchange: HttpExchange) {
        received +=
            Request(System.nanoTime(), exchange.requestURI.toString(), exchange.requestHeaders)
        val queued = queue.poll()
        if (queued != null) {
            last = queued
        }
        val response = last
        if (response.isDropped) {
            exchange.close()
            return
        }
        response.headers.forEach { (name, value) -> exchange.responseHeaders.add(name, value) }
        exchange.sendResponseHeaders(
            response.status,
            if (response.body.isEmpty()) -1 else response.body.size.toLong(),
        )
        exchange.responseBody.use { it.write(response.body) }
    }

    class Request(
        val nanoTime: Long,
        val path: String,
        val headers: Headers,
    )

    class Response(
        val status: Int,
        val body: ByteArray = ByteArray(0),
        val headers: Map<String, String> = emptyMap(),
        // Closes the connection without a response, so the client sees an I/O error.
        val isDropped: Boolean = false,
    ) {
        fun withHeader(
            name: String,
            value: String,
        ): Response = Response(status, body, headers + (name to value), isDropped)

        companion object {
            fun ok(body: String): Response = status(200, body)

            fun status(
                status: Int,
                body: String,
            ): Response = Response(status, body.toByteArray())

            fun gzip(body: String): Response {
                val bytes = ByteArrayOutputStream()
                GZIPOutputStream(bytes).use { it.write(body.toByteArray()) }
                return Response(200, bytes.toByteArray(), mapOf("Content-Encoding" to "gzip"))
            }

            fun dropped(): Response = Response(0, isDropped = true)
        }
    }
}
