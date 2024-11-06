package rip.sunrise.server.http

import com.sun.net.httpserver.HttpServer
import rip.sunrise.server.config.Config
import java.net.InetSocketAddress
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.random.Random

class JarHttpServer(private val port: Int, val config: Config) {
    private val endpoints = mutableMapOf<String, Int>()
    private val started = false

    fun start() {
        assert(!started)

        println("Starting HTTP Server")

        val server = HttpServer.create(InetSocketAddress(port), 0)
        server.createContext("/") { exchange ->
            val path = exchange.requestURI.path.removePrefix("/")

            val scriptId = endpoints[path] ?: run {
                exchange.sendResponseHeaders(404, 0)
                exchange.responseBody.close()
                return@createContext
            }

            val bytes = config.getScript(scriptId).bytes

            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.apply {
                write(bytes)
                close()
            }
        }

        loadEndpoints()

        server.executor = null
        server.start()
    }

    fun loadEndpoints() {
        endpoints.clear()
        config.scripts.forEach { script ->
            registerEndpoint(script.metadata.d)
        }
    }

    fun getScriptEndpoint(scriptId: Int): String {
        val entry = endpoints.entries.firstOrNull { (_, value) -> value == scriptId }
            ?: error("Couldn't find endpoint for script $scriptId")

        return entry.key
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun registerEndpoint(scriptId: Int): String {
        val path = Base64.UrlSafe.encode(Random.nextBytes(16))

        return path.also {
            endpoints[it] = scriptId
        }
    }
}