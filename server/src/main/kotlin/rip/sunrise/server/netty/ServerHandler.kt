package rip.sunrise.server.netty

import com.google.gson.Gson
import com.google.gson.JsonObject
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import org.dreambot.*
import org.msgpack.core.MessagePack
import rip.sunrise.packets.clientbound.*
import rip.sunrise.packets.msgpack.LOGIN_REQUEST_PACKET_ID
import rip.sunrise.packets.msgpack.LoginResponse
import rip.sunrise.packets.msgpack.unpackLoginRequest
import rip.sunrise.packets.serverbound.*
import rip.sunrise.server.config.Config
import rip.sunrise.server.http.JarHttpServer
import rip.sunrise.server.utils.extensions.md5sum
import java.util.Base64

const val ACCOUNT_SESSION_ID = "cMU/vYTQnRyD2cFx1i1J6aa+ZpRIINh5qkMxoTh8XoA"
const val SCRIPT_SESSION_ID = "dbsVbKA4mRLE4NaOMXCCnvPYEJsNsXdwek6hosbCiQ0"
const val SESSION_TOKEN = "DHGbNt9CZ2v6yNSPvsEq/zv/toLQmA7ET4Kdvq2xeZVol8UMMSyHk0QCyfyRHPf7hhvGvO/CA7M="
const val USER_ID = 1

val SCRIPT_AES_KEY = ByteArray(32) { 0 }
val SCRIPT_IV = ByteArray(16) { 0 }

class ServerHandler(private val config: Config, private val http: JarHttpServer) : SimpleChannelInboundHandler<Any>() {
    private val sessions = mutableMapOf<ChannelHandlerContext, Int>()

    override fun channelRead0(ctx: ChannelHandlerContext, msg: Any) {
        println("Got message: $msg")
        when (msg) {
            is String -> {
                handleJson(ctx, msg)
            }

            is ByteArray -> {
                MessagePack.newDefaultUnpacker(msg).use { unpacker ->
                    val id = unpacker.unpackByte()
                    println("Got message: $id")

                    when (id) {
                        LOGIN_REQUEST_PACKET_ID -> {
                            sessions[ctx] = -1

                            val packet = unpacker.unpackLoginRequest()
                            println(packet)
                            ctx.writeAndFlush(
                                LoginResponse(
                                    packet.username,
                                    ACCOUNT_SESSION_ID,
                                    SESSION_TOKEN,
                                    USER_ID,
                                    hashSetOf(10)
                                ).pack(0) // TODO: Not fully sure if it only increments on send
                            )
                        }
                    }
                }
            }

            is EncryptedScriptRequest -> {
                val endpoint = http.getScriptEndpoint(msg.f)
                val serverUrl = config.serverUrl.removeSuffix("/")

                @OptIn(ExperimentalStdlibApi::class)
                val checksum = http.getEncryptedScript(msg.f).md5sum().toHexString()

                val script = config.getScript(msg.f)

                sessions[ctx] = msg.f
                ctx.writeAndFlush(EncryptedScriptResp("$serverUrl/$endpoint", sanitizeName(script.metadata.m), checksum, Base64.getEncoder().encodeToString(SCRIPT_AES_KEY), -1))
            }

            is RevisionInfoRequest -> {
                // Some sort of pure JSON request
                ctx.writeAndFlush(JsonObject().apply {
                    addProperty("m", "a")
                }.toString())
            }

            is FreeScriptListRequest -> ctx.writeAndFlush(ScriptListResp(emptyList()))
            is PaidScriptListRequest -> ctx.writeAndFlush(ScriptListResp(config.scripts.map { it.metadata }))

            is ScriptSessionRequest -> ctx.writeAndFlush(ScriptSessionResp(0, SCRIPT_SESSION_ID))

            is ScriptStartRequest -> ctx.writeAndFlush(ScriptStartResp(false))

            is a5 -> ctx.writeAndFlush(am(6))

            // TODO: I don't think this exists anymore
            is af -> ctx.writeAndFlush(am(6))

            is aY -> ctx.writeAndFlush(bs(USER_ID))

            is GetActiveInstancesRequest -> ctx.writeAndFlush(GetInstancesResp(0))
            is GetTotalInstancesRequest -> ctx.writeAndFlush(GetInstancesResp(1))

            is ScriptOptionsRequest -> {
                val scriptId = sessions[ctx] ?: error("Couldn't find session $ctx")

                val options = config.getScript(scriptId).options
                    .map { it.split("=") }
                    .map { (key, value) -> key to encryptOption(value.toInt(), SCRIPT_SESSION_ID, USER_ID) }
                    .joinToString(",") { (key, value) -> "$key=$value" }

                ctx.writeAndFlush(ScriptOptionsResp(options))
            }

            else -> error("Unknown packet $msg")
        }
    }

    fun handleJson(ctx: ChannelHandlerContext, msg: String) {
        println("Got JSON Message!")

        val json = Gson().fromJson(msg, JsonObject::class.java)
        val code = json.get("m").asString

        when (code) {
            // Sent when requesting revision info.
            "a" -> {
                ctx.writeAndFlush(RevisionInfoResp(config.revisionData))
            }
            "b" -> TODO()
            "z" -> TODO()
        }
    }

    private fun encryptOption(value: Int, scriptSessionId: String, userId: Int): Int {
        return value xor scriptSessionId.hashCode() xor userId
    }

    private fun sanitizeName(name: String): String {
        return name.replace(" ", "_").replace("[^A-Za-z0-9_]".toRegex(), "")
    }
}