package rip.sunrise.server.config

import rip.sunrise.server.http.JarHttpServer
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.util.concurrent.TimeUnit

class ConfigWatcher(private val configDir: Path, private val config: Config, private val http: JarHttpServer) : Runnable {
    override fun run() {
        val service = FileSystems.getDefault().newWatchService()

        configDir.register(
            service,
            StandardWatchEventKinds.ENTRY_MODIFY,
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_DELETE
        )

        while (true) {
            val key = service.take()
            // Wait for the files to stabilize
            TimeUnit.SECONDS.sleep(1)

            if (key.pollEvents().isNotEmpty()) {
                println("File changed, reloading config!")

                config.load()
                http.loadEndpoints()
            }

            key.reset()
        }
    }
}