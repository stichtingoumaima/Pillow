package rip.sunrise.client

import com.google.gson.Gson
import com.google.gson.JsonObject
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import org.msgpack.core.MessagePack
import rip.sunrise.client.config.ScriptConfig
import rip.sunrise.client.utils.extensions.decryptScript
import rip.sunrise.packets.clientbound.*
import rip.sunrise.packets.msgpack.LOGIN_RESPONSE_PACKET_ID
import rip.sunrise.packets.msgpack.LoginRequest
import rip.sunrise.packets.msgpack.unpackLoginResponse
import rip.sunrise.packets.serverbound.*
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import kotlin.concurrent.thread
import kotlin.io.path.Path

class ClientHandler(val username: String, val password: String, val hardwareId: String) :
    ChannelInboundHandlerAdapter() {
    private lateinit var accountSession: String
    private lateinit var scriptSession: String
    private var userId: Int = -1

    private val queue = ArrayBlockingQueue<Any>(1)

    override fun channelActive(ctx: ChannelHandlerContext) {
        println("Open")

        // TODO: Use the session token, if possible.
        ctx.writeAndFlush(LoginRequest(0, username, password, "", DBClientData.sharedSecret, hardwareId).pack())
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

            is ByteArray -> {
                println(msg.contentToString())

                val unpacker = MessagePack.newDefaultUnpacker(msg)

                val id = unpacker.unpackInt()
                println("Got packet ID $id")

                when (id) {
                    LOGIN_RESPONSE_PACKET_ID -> {
                        val response = unpacker.unpackLoginResponse()
                        println("Got login response: $response")

                        userId = response.userId
                        accountSession = response.accountSessionToken

                        // BANNED and BANNED_2
                        if (5 in response.ranks || 42 in response.ranks) error("This account is banned. Change the IP before making a new one.")

                        if (userId <= 0 || accountSession.isEmpty()) {
                            error("Something went wrong logging in! Try changing the HARDWARE_ID, IP, or account. $msg")
                        }

                        ctx.writeAndFlush(RevisionInfoRequest(hardwareId, DBClientData.sharedSecret))
                    }
                }
            }

            is LoginResp -> {
                userId = msg.k
                accountSession = msg.p

                // BANNED and BANNED_2
                if (5 in msg.a || 42 in msg.a) error("This account is banned. Change the IP before making a new one.")

                if (userId <= 0 || accountSession.isEmpty()) {
                    error("Something went wrong logging in! Try changing the HARDWARE_ID, IP, or account. $msg")
                }

                ctx.writeAndFlush(RevisionInfoRequest(hardwareId, DBClientData.sharedSecret))
            }

            is RevisionInfoResp -> {
                if (msg.e == null) {
                    error("Failed to get revision info. The constant or HARDWARE_ID might be incorrect.")
                }

                writeRevisionData(msg.e!!)
                println("Revision data written")

                ctx.writeAndFlush(ScriptSessionRequest("$accountSession:MID:$hardwareId"))
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
                obj.addProperty("r", DBClientData.hash)
                obj.addProperty("c", hardwareId)
                obj.addProperty("e", "")
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
            it.writeText(gson.toJson(ScriptConfig(
                name = script.m,
                description = script.w,
                version = script.e,
                author = script.l,
                imageUrl = script.q,
                threadUrl = script.v,

                jarFile = "jars/$name.jar",
                optionFile = "options/$name.txt",
            )))
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