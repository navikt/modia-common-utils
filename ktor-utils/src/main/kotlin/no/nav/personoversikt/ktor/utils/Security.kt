package no.nav.personoversikt.ktor.utils

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.Payload
import io.ktor.http.*
import io.ktor.http.auth.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import no.nav.personoversikt.common.crypto.Crypter
import no.nav.personoversikt.common.utils.StringUtils.addPrefixIfMissing
import no.nav.personoversikt.common.utils.StringUtils.removePrefix
import org.slf4j.LoggerFactory
import java.net.URL

class Security(private val providers: List<AuthProviderConfig>) {
    constructor(vararg providers: AuthProviderConfig) : this(providers.asList())
    companion object {
        private val logger = LoggerFactory.getLogger(Security::class.java)
        const val UNAUTHENTICATED = "Unauthenticated"
        const val JWT_PARSE_ERROR = "JWT parse error"
    }

    sealed interface JwksConfig {
        val jwksUrl: String
        val issuer: String

        class JwksUrl(override val jwksUrl: String, override val issuer: String) : JwksConfig
        class OidcWellKnownUrl(url: String) : JwksConfig {
            private val oidcClient = OidcClient(url)

            private val config: OidcClient.OidcDiscoveryConfig by lazy { runBlocking(Dispatchers.IO) { oidcClient.fetch() } }

            override val jwksUrl: String by lazy { config.jwksUrl }
            override val issuer: String by lazy { config.issuer }
        }
    }
    data class AuthProviderConfig(
        val name: String?,
        val jwksConfig: JwksConfig,
        val tokenLocations: List<TokenLocation> = emptyList(),
        val overrides: JWTAuthenticationProvider.Config.() -> Unit = {}
    )

    sealed interface TokenLocation {
        val encryptionKey: String?
        fun extract(call: ApplicationCall): String?
        fun getToken(call: ApplicationCall, cryptermap: Map<String, Crypter>): String? {
            val value = extract(call) ?: return null
            val crypter = cryptermap[encryptionKey] ?: return value
            return crypter.decrypt(value).getOrNull()
        }

        class Cookie(
            private val name: String,
            override val encryptionKey: String? = null
        ) : TokenLocation {
            override fun extract(call: ApplicationCall): String? = call.request.cookies[name]
        }

        class Header(
            private val headerName: String = HttpHeaders.Authorization,
            override val encryptionKey: String? = null
        ) : TokenLocation {
            override fun extract(call: ApplicationCall): String? = call.request.header(headerName)
        }
    }

    class SubjectPrincipal(val token: String, val payload: Payload) : Principal {
        constructor(token: String) : this(token, JWT.decode(token))

        val subject: String? = payload.getClaim("NAVident")?.asString() ?: payload.subject
    }

    private val cryptermap = providers
        .flatMap { it.tokenLocations }
        .mapNotNull { it.encryptionKey }
        .associateWith { Crypter(it) }

    val authproviders: Array<String?> = providers.map { it.name }.toTypedArray()

    fun getSubject(call: ApplicationCall): List<String> {
        return providers.map { getSubject(call, it) }
    }

    fun getToken(call: ApplicationCall): List<String?> {
        return providers.map { getToken(call, it) }
    }

    fun setupMock(context: AuthenticationConfig, subject: String) {
        val token = JWT.create().withSubject(subject).sign(Algorithm.none())
        val principal = SubjectPrincipal(token)

        for (provider in providers) {
            context.provider(provider.name) {
                authenticate {
                    it.principal(principal)
                }
            }
        }
    }

    fun setupJWT(context: AuthenticationConfig) {
        for (provider in providers) {
            context.jwt(provider.name) {
                authHeader {
                    parseAuthorizationHeader(getToken(it, provider) ?: "")
                }
                verifier(makeJwkProvider(provider.jwksConfig.jwksUrl))
                validate { validateJWT(it, provider) }
                apply(provider.overrides)
            }
        }
    }

    private fun getSubject(call: ApplicationCall, provider: AuthProviderConfig): String {
        return try {
            getToken(call, provider)
                ?.removePrefix("Bearer ", ignoreCase = true)
                ?.let(JWT::decode)
                ?.let(::JWTCredential)
                ?.let { call.validateJWT(it, provider) }
                ?.subject
                ?: UNAUTHENTICATED
        } catch (e: Throwable) {
            JWT_PARSE_ERROR
        }
    }

    private fun getToken(call: ApplicationCall, provider: AuthProviderConfig): String? {
        return provider.tokenLocations.firstNotNullOfOrNull { location ->
            location.getToken(call, cryptermap)
                ?.addPrefixIfMissing("Bearer ", ignoreCase = true)
        }
    }

    private fun makeJwkProvider(jwksUrl: String): JwkProvider {
        return JwkProviderBuilder(URL(jwksUrl))
            .cached(true)
            .rateLimited(true)
            .build()
    }

    private fun ApplicationCall.validateJWT(credentials: JWTCredential, provider: AuthProviderConfig): SubjectPrincipal? {
        return try {
            checkNotNull(credentials.payload.audience) { "Audience was not present in jwt" }
            check(credentials.payload.issuer == provider.jwksConfig.issuer) {
                "Issuer did not match provider config. Expected: '${provider.jwksConfig.issuer}', but got: '${credentials.payload.issuer}'"
            }
            val token = checkNotNull(getToken(this, provider)) { "Could not get JWT for provider '${provider.name}'" }
            val principal = SubjectPrincipal(token = token, payload = credentials.payload)
            checkNotNull(principal.subject) { "Could not get subject from jwt" }
            principal
        } catch (e: Throwable) {
            logger.error("Failed to validate JWT", e)
            null
        }
    }
}
