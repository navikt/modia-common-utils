package no.nav.personoversikt.common.ktor.utils

import io.ktor.server.application.*
import io.ktor.server.engine.*
import org.slf4j.LoggerFactory

object KtorServer {
    private val logger = LoggerFactory.getLogger(KtorServer::class.java)

    fun <TEngine : ApplicationEngine, TConfiguration : ApplicationEngine.Configuration> create(
        factory: ApplicationEngineFactory<TEngine, TConfiguration>,
        port: Int = 8080,
        configure: TConfiguration.() -> Unit = {},
        application: Application.() -> Unit,
    ): EmbeddedServer<TEngine, TConfiguration> {
        val server =
            embeddedServer(
                factory,
                serverConfig(applicationEnvironment()) {
                    module {
                        application()
                    }
                },
                configure = {
                    connectors.add(
                        EngineConnectorBuilder().apply {
                            this.port = port
                        },
                    )
                    configure()
                },
            )

        Runtime.getRuntime().addShutdownHook(
            Thread {
                logger.info("Shutdown hook called, shutting down gracefully")
                server.stop(5000, 5000)
            },
        )

        return server
    }
}
