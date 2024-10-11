package no.nav.personoversikt.common.ktor.utils

import io.ktor.server.application.*
import io.ktor.server.engine.*
import org.slf4j.LoggerFactory

object KtorServer {
    private val logger = LoggerFactory.getLogger(KtorServer::class.java)
    fun <TEngine : ApplicationEngine, TConfiguration : ApplicationEngine.Configuration> create(
        factory: ApplicationEngineFactory<TEngine, TConfiguration>,
        port: Int = 8080,
        application: Application.() -> Unit,
    ): EmbeddedServer<TEngine, TConfiguration> {
        val server = embeddedServer(factory, port) {
            application()
        }

        Runtime.getRuntime().addShutdownHook(
            Thread {
                logger.info("Shutdown hook called, shutting down gracefully")
                server.stop(5000, 5000)
            }
        )

        return server
    }
}
