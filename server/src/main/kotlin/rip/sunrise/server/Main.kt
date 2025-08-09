package rip.sunrise.server

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelOption
import io.netty.channel.MultiThreadIoEventLoopGroup
import io.netty.channel.nio.NioIoHandler
import io.netty.channel.socket.nio.NioServerSocketChannel
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import rip.sunrise.server.config.Config
import rip.sunrise.server.config.ConfigWatcher
import rip.sunrise.server.http.JarHttpServer
import rip.sunrise.server.netty.ServerInitializer
import kotlin.concurrent.thread
import kotlin.io.path.Path

const val HTTP_PORT = 6666
const val NETTY_PORT = 1337
const val CONFIG_ENV = "CONFIG_DIR"

val logger: Logger = LoggerFactory.getLogger("Server")

fun main() {
    val configDir = Path(System.getenv(CONFIG_ENV))

    val config = Config(configDir).also {
        it.load()
    }

    val http = JarHttpServer(HTTP_PORT, config).apply {
        start()
    }

    thread(isDaemon = true, name = "Config Watcher Thread") {
        ConfigWatcher(configDir, config, http).run()
    }

    val initializer = ServerInitializer(config, http)

    val group = MultiThreadIoEventLoopGroup(NioIoHandler.newFactory())
    try {
        val bootstrap = ServerBootstrap()
        bootstrap.group(group)
            .channel(NioServerSocketChannel::class.java)
            .option(ChannelOption.SO_BACKLOG, 128)
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            .childHandler(initializer)

        val f = bootstrap.bind(NETTY_PORT).sync()
        logger.info("Started Netty Server on port {}", NETTY_PORT)

        f.channel().closeFuture().sync()
    } finally {
        group.shutdownGracefully()
    }
}