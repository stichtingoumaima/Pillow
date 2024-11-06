package rip.sunrise.client

import com.google.gson.Gson
import com.google.gson.JsonObject
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import rip.sunrise.packets.clientbound.*
import rip.sunrise.packets.serverbound.*
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.concurrent.ArrayBlockingQueue
import kotlin.concurrent.thread
import kotlin.io.path.Path

class ClientHandler : ChannelInboundHandlerAdapter() {
    private val SOME_CONSTANT = "f95cf4001d19fc517ccc94"
    private val OTHER_CONSTANT = "3b40bcda6a4fe1c577ae30b3212d16b6"

    private lateinit var accountSession: String
    private lateinit var scriptSession: String
    private var userId: Int = -1

    private val queue = ArrayBlockingQueue<String>(1)

    override fun channelActive(ctx: ChannelHandlerContext) {
        println("Open")

        ctx.writeAndFlush(LoginRequest("gangie425", "siemagang1234!", SOME_CONSTANT))
    }

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        when (msg) {
            is LoginResp -> {
                accountSession = msg.d
                userId = msg.a

                ctx.writeAndFlush(RevisionInfoRequest(OTHER_CONSTANT, SOME_CONSTANT))
                ctx.writeAndFlush(ScriptSessionRequest("$accountSession:MID:$OTHER_CONSTANT"))
            }

            is RevisionInfoResp -> {
                writeRevisionData(msg.e)
                println("Revision data written")
            }

            is ScriptSessionResp -> {
                scriptSession = msg.c

                // NOTE: Change if you want to dump free scripts
                ctx.writeAndFlush(PaidScriptListRequest(accountSession))
            }

            is ScriptListResp -> {
                println("Scripts owned: ${msg.v.size}")
                // TODO: This is very ugly
                thread {
                    msg.v.forEach { script ->
                        ctx.writeAndFlush(ScriptURLRequest(script.x, accountSession, scriptSession))
                        // Wait for URL response
                        while (queue.isEmpty()) { }
                        val url = queue.poll() as String

                        ctx.writeAndFlush(ScriptOptionsRequest(accountSession, scriptSession))
                        while (queue.isEmpty()) { }
                        val options = queue.poll() as String

                        println("Writing")
                        writeScriptData(script, url, options)
                        println("Script written")
                    }

                    ctx.disconnect()
                }
            }

            is ScriptURLResp -> {
                queue.put(msg.w)
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

            else -> error("Unknown message $msg")
        }
    }

    private fun writeRevisionData(data: String) {
        val outputPath = Path("output")

        outputPath.resolve("revision.txt").toFile().also {
            it.parentFile.mkdirs()
            it.createNewFile()
            it.writeText(data)
        }
    }

    private fun writeScriptData(script: ScriptWrapper, url: String, options: String) {
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

        val bytes = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI(url)).build(), HttpResponse.BodyHandlers.ofInputStream()).body()
        outputPath.resolve("jars/$name.jar").toFile().also {
            it.parentFile.mkdirs()
            it.createNewFile()
            it.writeBytes(bytes.readBytes())
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