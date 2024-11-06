package rip.sunrise.server

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import rip.sunrise.server.config.Config
import rip.sunrise.server.config.ConfigWatcher
import rip.sunrise.server.http.JarHttpServer
import rip.sunrise.server.netty.ServerInitializer
import kotlin.concurrent.thread
import kotlin.io.path.Path

const val HTTP_PORT = 6666
const val NETTY_PORT = 1337
const val CONFIG_ENV = "CONFIG_DIR"

fun main() {
    val configDir = Path(System.getenv(CONFIG_ENV))
    val configFile = configDir.resolve("config.json").toFile()

    val config = Config(configFile)
    val http = JarHttpServer(HTTP_PORT, config).apply {
        start()
    }

    thread(isDaemon = true, name = "Config Watcher Thread") {
        ConfigWatcher(configDir, config, http).run()
    }

    val initializer = ServerInitializer(config, http)

    val group = NioEventLoopGroup()
    try {
        val bootstrap = ServerBootstrap()
        bootstrap.group(group)
            .channel(NioServerSocketChannel::class.java)
            .option(ChannelOption.SO_BACKLOG, 128)
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            .childHandler(initializer)

        println("Starting Netty Server")
        val f = bootstrap.bind(NETTY_PORT).sync()

        f.channel().closeFuture().sync()
    } finally {
        group.shutdownGracefully()
    }
}