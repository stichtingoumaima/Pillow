package rip.sunrise.server.http

import com.sun.net.httpserver.HttpServer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import rip.sunrise.server.config.Config
import rip.sunrise.server.netty.SCRIPT_AES_KEY
import rip.sunrise.server.netty.SCRIPT_IV
import rip.sunrise.server.utils.extensions.encryptScriptAES
import java.net.InetSocketAddress
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.random.Random

class JarHttpServer(private val port: Int, val config: Config) {
    private val endpoints = mutableMapOf<String, Int>()

    private val started = false

    fun start() {
        assert(!started)

        val server = HttpServer.create(InetSocketAddress(port), 0)
        server.createContext("/") { exchange ->
            val path = exchange.requestURI.path.removePrefix("/")

            val scriptId = endpoints[path] ?: run {
                exchange.sendResponseHeaders(404, 0)
                exchange.responseBody.close()
                return@createContext
            }

            val bytes = getEncryptedScript(scriptId)

            runCatching {
                exchange.sendResponseHeaders(200, bytes.size.toLong())
                exchange.responseBody.use {
                    it.write(bytes)
                }

                logger.info("Sent HTTP response for scriptId {}", scriptId)
            }.onFailure {
                logger.error("Failed to send HTTP response for scriptId $scriptId", it)
            }
        }

        loadEndpoints()

        server.executor = null
        server.start()

        logger.info("Started HTTP server on port {}", port)
    }

    fun loadEndpoints() {
        endpoints.clear()
        config.scripts.forEach { script ->
            registerEndpoint(script.metadata.x)
        }
    }

    fun getScriptEndpoint(scriptId: Int): String {
        val entry = endpoints.entries.firstOrNull { (_, value) -> value == scriptId }
            ?: error("Couldn't find endpoint for script $scriptId")

        return entry.key
    }

    // TODO: No caching is also bad, because it's being loaded twice.
    fun getEncryptedScript(scriptId: Int): ByteArray {
        val jarBytes = config.getScript(scriptId).scriptJar.readBytes()
        return jarBytes.encryptScriptAES(SCRIPT_AES_KEY, SCRIPT_IV)
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun registerEndpoint(scriptId: Int): String {
        return Base64.UrlSafe.encode(Random.nextBytes(16)).also {
            endpoints[it] = scriptId
        }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger("HttpServer")
    }
}