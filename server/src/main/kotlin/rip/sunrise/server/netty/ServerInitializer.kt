package rip.sunrise.server.netty

import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import io.netty.handler.codec.LengthFieldPrepender
import io.netty.handler.codec.compression.ZlibCodecFactory
import io.netty.handler.codec.compression.ZlibWrapper
import io.netty.handler.codec.serialization.ObjectDecoder
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.timeout.ReadTimeoutHandler
import rip.sunrise.packets.serialization.ObfuscatedClassResolver
import rip.sunrise.packets.serialization.ObfuscatedEncoder
import rip.sunrise.server.config.Config
import rip.sunrise.server.http.JarHttpServer

class ServerInitializer(private val config: Config, private val http: JarHttpServer) : ChannelInitializer<SocketChannel>() {
    override fun initChannel(ch: SocketChannel) {
        val pipeline = ch.pipeline()

        pipeline.addLast(createSSLContext().newHandler(ch.alloc()))

        pipeline.addLast(ZlibCodecFactory.newZlibEncoder(ZlibWrapper.ZLIB));
        pipeline.addLast(ZlibCodecFactory.newZlibDecoder(ZlibWrapper.ZLIB));

        pipeline.addLast(LengthFieldBasedFrameDecoder(Int.MAX_VALUE, 0, 4, 0, 4))
        pipeline.addLast(LengthFieldPrepender(4))

        pipeline.addLast(ObfuscatedEncoder)
        pipeline.addLast(ObjectDecoder(ObfuscatedClassResolver))

        pipeline.addLast(ReadTimeoutHandler(600))

        pipeline.addLast(ServerHandler(config, http))
    }

    private fun createSSLContext(): SslContext {
        val certChain = this::class.java.getResourceAsStream("/server.crt")
        val privateKey = this::class.java.getResourceAsStream("/server.key")

        return SslContextBuilder.forServer(certChain, privateKey).build()
    }
}