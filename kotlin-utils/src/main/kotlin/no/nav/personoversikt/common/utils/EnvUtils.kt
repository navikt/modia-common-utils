package no.nav.personoversikt.common.utils

import java.nio.file.Paths
import kotlin.io.path.readText

object EnvUtils {
    fun getConfig(name: String, defaultValues: Map<String, String?> = emptyMap()): String? {
        return System.getProperty(name, System.getenv(name) ?: defaultValues[name])
    }

    fun getRequiredConfig(name: String, defaultValues: Map<String, String?> = emptyMap()): String = checkNotNull(
        getConfig(name, defaultValues)
    ) {
        "Could not find property or environment variable for '$name'"
    }

    const val defaultVaultPath = "/var/run/secrets/nais.io"

    class Credential(val username: String, val password: String)

    fun readVaultCredential(name: String, vaultPath: String = defaultVaultPath): Credential {
        val path = Paths.get(vaultPath, name)
        val username = path.resolve("username").readText()
        val password = path.resolve("password").readText()
        return Credential(username, password)
    }
}
