package rip.sunrise.client

import com.google.gson.Gson
import com.google.gson.JsonObject
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import rip.sunrise.client.utils.extensions.decryptScript
import rip.sunrise.packets.serverbound.EncryptedScriptRequest
import rip.sunrise.packets.clientbound.EncryptedScriptResp
import rip.sunrise.packets.clientbound.*
import rip.sunrise.packets.serverbound.*
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.Base64
import java.util.concurrent.ArrayBlockingQueue
import kotlin.concurrent.thread
import kotlin.io.path.Path

// Literally constant in the DreamBot jar.
// TODO: Monitor changes to this on updates.
private const val SOME_CONSTANT = "03c7c0ace395d80182db07"

// sha256sum of client.jar
// TODO: Auto-Resolve
private const val CLIENT_SHA256 = "98417e7c2ae1013d06ebf2cfe6e1fbabb3b5724053a4c13cdef2cf764d317f5c"

class ClientHandler(val username: String, val password: String, val hardwareId: String) :
    ChannelInboundHandlerAdapter() {
    private lateinit var accountSession: String
    private lateinit var scriptSession: String
    private var userId: Int = -1

    private val queue = ArrayBlockingQueue<Any>(1)

    override fun channelActive(ctx: ChannelHandlerContext) {
        println("Open")

        ctx.writeAndFlush(LoginRequest(username, password, SOME_CONSTANT))
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        ctx.disconnect()
        throw RuntimeException(cause)
    }

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        when (msg) {
            is String -> {
                handleJson(ctx, msg)
            }

            is LoginResp -> {
                userId = msg.a
                accountSession = msg.d

                if (userId <= 0 || accountSession.isEmpty()) {
                    error("Something went wrong logging in! Try changing the HARDWARE_ID, IP, or account. $msg")
                }

                ctx.writeAndFlush(RevisionInfoRequest(hardwareId, SOME_CONSTANT))
                ctx.writeAndFlush(ScriptSessionRequest("$accountSession:MID:$hardwareId"))
            }

            is RevisionInfoResp -> {
                if (msg.e == null) {
                    error("Failed to get revision info. The constant or HARDWARE_ID might be incorrect.")
                }

                writeRevisionData(msg.e!!)
                println("Revision data written")
            }

            is ScriptSessionResp -> {
                if (msg.c == null) {
                    error("Failed to get script session. The HARDWARE_ID might be incorrect.")
                }
                scriptSession = msg.c!!

                // NOTE: Change if you want to dump free scripts
                ctx.writeAndFlush(PaidScriptListRequest(accountSession))
            }

            is ScriptListResp -> {
                println("Scripts owned: ${msg.v.size}")
                // TODO: This is very ugly
                thread {
                    msg.v.forEach { script ->
                        ctx.writeAndFlush(EncryptedScriptRequest(script.x, accountSession, scriptSession))
                        // Wait for URL response
                        while (queue.isEmpty()) { }
                        val data = queue.poll() as ByteArray

                        ctx.writeAndFlush(ScriptOptionsRequest(accountSession, scriptSession))
                        while (queue.isEmpty()) { }
                        val options = queue.poll() as String

                        println("Writing")
                        writeScriptData(script, data, options)
                        println("Script written")
                    }

                    ctx.disconnect()
                }
            }

            is EncryptedScriptResp -> {
                val encrypted = HttpClient.newHttpClient()
                    .send(
                        HttpRequest.newBuilder(URI(msg.w)).build(),
                        HttpResponse.BodyHandlers.ofInputStream()
                    )
                    .body()
                    .readAllBytes()

                val bytes = msg.z?.let {
                    encrypted.decryptScript(Base64.getDecoder().decode(it))
                } ?: encrypted

                queue.put(bytes)
            }

            is ScriptOptionsResp -> {
                if (msg.c.isBlank()) {
                    queue.put("")
                } else {
                    val options =
                        msg.c.trim().split(",").map { p -> p.split("=") }.joinToString(separator = "\n") { (key, value) ->
                            "$key=${value.toInt() xor scriptSession.hashCode() xor userId}"
                        }
                    queue.put(options)
                }
            }

            else -> println("Unknown message $msg")
        }
    }

    fun handleJson(ctx: ChannelHandlerContext, msg: String) {
        val json = Gson().fromJson(msg, JsonObject::class.java)
        val code = json.get("m").asString

        val obj = JsonObject().apply {
            addProperty("m", code)
        }
        when (code) {
            // Sent when requesting revision info.
            "a" -> {
                obj.addProperty("r", CLIENT_SHA256)
                obj.addProperty("c", hardwareId)
            }
            "b" -> TODO()
            "z" -> TODO()
        }

        ctx.writeAndFlush(obj.toString())
    }

    private fun writeRevisionData(data: String) {
        val outputPath = Path("output")

        outputPath.resolve("revision.txt").toFile().also {
            it.parentFile.mkdirs()
            it.createNewFile()
            it.writeText(data)
        }
    }

    private fun writeScriptData(script: ScriptWrapper, data: ByteArray, options: String) {
        val name = sanitizeName(script.m)

        val gson = Gson().newBuilder().setPrettyPrinting().create()

        val outputPath = Path("output")

        outputPath.resolve("configs/$name.json").toFile().also {
            it.parentFile.mkdirs()
            it.createNewFile()
            it.writeText(gson.toJson(JsonObject().apply {
                addProperty("name", script.m)
                addProperty("description", script.w)
                addProperty("version", script.e)
                addProperty("author", script.l)
                addProperty("imageUrl", script.q)
                addProperty("jarFile", "jars/$name.jar")
                addProperty("optionFile", "options/$name.txt")
            }))
        }

        outputPath.resolve("jars/$name.jar").toFile().also {
            it.parentFile.mkdirs()
            it.createNewFile()
            it.writeBytes(data)
        }

        outputPath.resolve("options/$name.txt").toFile().also {
            it.parentFile.mkdirs()
            it.createNewFile()
            it.writeText(options)
        }

        println("Done")
    }

    private fun sanitizeName(name: String): String {
        return name.replace(" ", "_").replace("[^A-Za-z0-9_]".toRegex(), "")
    }
}