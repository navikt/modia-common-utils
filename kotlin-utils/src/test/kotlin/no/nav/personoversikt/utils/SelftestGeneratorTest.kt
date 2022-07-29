package no.nav.personoversikt.utils

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class SelftestGeneratorTest {
    @BeforeEach
    internal fun setup() {
        SelftestGenerator.restart()
    }

    @AfterEach
    internal fun teardown() {
        SelftestGenerator.stop()
    }

    @Test
    internal fun `should not be ready before every dependency has reported ok`() {
        val selftest = SelftestGenerator(SelftestGenerator.Config(appname = "testapp", version = "1.0.0"))
        val reporter = SelftestGenerator.Reporter("dependency", critical = false)

        selftest.assertStatus(isAlive = true, isReady = false)

        report {
            reporter.reportOk()
        }

        selftest.assertStatus(isAlive = true, isReady = true)
    }

    @Test
    internal fun `should mark as not alive if critical errors occur`() {
        val selftest = SelftestGenerator(SelftestGenerator.Config(appname = "testapp", version = "1.0.0"))
        val nonCritical = SelftestGenerator.Reporter("dependency", critical = false)
        val critical = SelftestGenerator.Reporter("other-dependency", critical = true)

        selftest.assertStatus(isAlive = true, isReady = false)

        report {
            nonCritical.reportOk()
            critical.reportOk()
        }

        selftest.assertStatus(isAlive = true, isReady = true)

        report {
            nonCritical.reportError(IllegalStateException("Something non-critical is wrong"))
            critical.reportOk()
        }

        selftest.assertStatus(isAlive = true, isReady = true)

        report {
            nonCritical.reportOk()
            critical.reportError(IllegalStateException("Something critical is wrong"))
        }

        selftest.assertStatus(isAlive = false, isReady = false)
    }

    @Test
    internal fun `should list all dependencies in selftest`() {
        val selftest = SelftestGenerator(SelftestGenerator.Config(appname = "testapp", version = "1.0.0"))
        val nonCritical = SelftestGenerator.Reporter("dependency", critical = false)
        val critical = SelftestGenerator.Reporter("other-dependency", critical = true)

        selftest.assertSelftestContent(
            """
                    Appname: testapp
                    Version: 1.0.0
                    
                    Name: dependency  Status: Registered
                    Name: other-dependency (Critical) Status: Registered
            """.trimIndent()
        )

        report {
            nonCritical.reportOk()
            critical.reportOk()
        }

        selftest.assertSelftestContent(
            """
                    Appname: testapp
                    Version: 1.0.0
                    
                    Name: dependency  Status: OK
                    Name: other-dependency (Critical) Status: OK
            """.trimIndent()
        )

        report {
            nonCritical.reportError(IllegalStateException("Non critical error"))
            critical.reportError(IllegalStateException("Critical error"))
        }

        selftest.assertSelftestContent(
            """
                Appname: testapp
                Version: 1.0.0
                
                Name: dependency  Status: KO: Non critical error
                Name: other-dependency (Critical) Status: KO: Critical error
            """.trimIndent()
        )
    }

    private fun SelftestGenerator.assertStatus(isAlive: Boolean, isReady: Boolean) {
        assertEquals(isAlive, this.isAlive())
        assertEquals(isReady, this.isReady())
    }

    private fun SelftestGenerator.assertSelftestContent(content: String) {
        assertEquals(content, this.scrape().trim())
    }

    private fun report(block: suspend () -> Unit) = runBlocking {
        block()
        delay(250)
    }
}
