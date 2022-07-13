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
import no.nav.personoversikt.crypto.Crypter
import no.nav.personoversikt.utils.StringUtils.addPrefixIfMissing
import no.nav.personoversikt.utils.StringUtils.removePrefix
import org.slf4j.LoggerFactory
import java.net.URL

class Security(private val providers: List<AuthProviderConfig>) {
    constructor(vararg providers: AuthProviderConfig) : this(providers.asList())

    companion object {
        const val UNAUTHENTICATED = "Unauthenticated"
        const val JWT_PARSE_ERROR = "JWT parse error"
    }
    class AuthProviderConfig(
        val name: String?,
        val jwksUrl: String,
        val cookies: List<AuthCookie> = emptyList()
    )
    class AuthCookie(
        val name: String,
        val encryptionKey: String? = null
    )
    class SubjectPrincipal(val payload: Payload) : Principal {
        val subject: String? = payload.getClaim("NAVident")?.asString() ?: payload.subject
    }

    private val logger = LoggerFactory.getLogger(Security::class.java)
    private val cryptermap = providers
        .flatMap { it.cookies }
        .mapNotNull { it.encryptionKey }
        .associateWith { Crypter(it) }

    val authproviders: Array<String?> = providers.map { it.name }.toTypedArray()

    fun getSubject(call: ApplicationCall): List<String> {
        return providers.map { getSubject(call, it.cookies) }
    }

    fun getToken(call: ApplicationCall): List<String?> {
        return providers.map { getToken(call, it.cookies) }
    }

    context(AuthenticationConfig)
    fun setupMock(subject: String) {
        val token = JWT.create().withSubject(subject).sign(Algorithm.none())
        val principal = SubjectPrincipal(JWT.decode(token))

        for (provider in providers) {
            val config = object : AuthenticationProvider.Config(provider.name) {}
            register(
                object : AuthenticationProvider(config) {
                    override suspend fun onAuthenticate(context: AuthenticationContext) {
                        context.principal = principal
                    }
                }
            )
        }
    }

    context(AuthenticationConfig)
    fun setupJWT() {
        for (provider in providers) {
            jwt {
                if (provider.cookies.isNotEmpty()) {
                    authHeader {
                        parseAuthorizationHeader(getToken(it, provider.cookies) ?: "")
                    }
                }
                verifier(makeJwkProvider(provider.jwksUrl))
                validate { validateJWT(it) }
            }
        }
    }

    private fun getSubject(call: ApplicationCall, cookies: List<AuthCookie>): String {
        return try {
            getToken(call, cookies)
                ?.removePrefix("Bearer ", ignoreCase = true)
                ?.let(JWT::decode)
                ?.let(::JWTCredential)
                ?.let(::validateJWT)
                ?.subject
                ?: UNAUTHENTICATED
        } catch (e: Throwable) {
            JWT_PARSE_ERROR
        }
    }

    private fun getToken(call: ApplicationCall, cookies: List<AuthCookie>): String? {
        return call.request.header(HttpHeaders.Authorization) ?: getTokenFromCookies(call, cookies)
    }

    private fun getTokenFromCookies(call: ApplicationCall, cookies: List<AuthCookie>): String? {
        return cookies
            .find { call.request.cookies[it.name]?.isNotEmpty() ?: false }
            ?.getValue(call)
            ?.addPrefixIfMissing("Bearer ", ignoreCase = true)
    }

    private fun makeJwkProvider(jwksUrl: String): JwkProvider {
        return JwkProviderBuilder(URL(jwksUrl))
            .cached(true)
            .rateLimited(true)
            .build()
    }

    private fun validateJWT(credentials: JWTCredential): SubjectPrincipal? {
        return try {
            checkNotNull(credentials.payload.audience) { "Audience was not present in jwt" }
            val principal = SubjectPrincipal(payload = credentials.payload)
            checkNotNull(principal.subject) { "Could not get subject from jwt" }
            principal
        } catch (e: Throwable) {
            logger.error("Failed to validate JWT", e)
            null
        }
    }

    private fun AuthCookie.getValue(call: ApplicationCall): String? {
        val value = call.request.cookies[this.name] ?: return null
        val crypter = cryptermap[this.encryptionKey] ?: return value
        return crypter.decrypt(value).getOrNull()
    }
}
