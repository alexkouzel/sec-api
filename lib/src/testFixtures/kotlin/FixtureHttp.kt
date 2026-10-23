package com.alexkouzel.sec

import com.alexkouzel.sec.http.Http

// Serves a fixture for each routed URL. Code builds its own URLs, so routing by exact URL
// checks them too: an unrouted URL fails like a failed request.
internal class FixtureHttp(
    vararg routes: Pair<String, String>,
) : Http {
    private val routes = routes.toMap(mutableMapOf())
    private val requests = mutableListOf<String>()

    val requestedUrls: List<String> get() = requests.toList()

    override var unknownFields: UnknownFieldListener? = null

    fun route(
        url: String,
        path: String,
    ): FixtureHttp = apply { routes[url] = path }

    override fun get(url: String): ByteArray {
        requests += url
        val path = routes[url] ?: throw SecException.Http("No fixture routed for $url", url)
        return Fixtures.bytes(path)
    }
}
