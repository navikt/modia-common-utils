package no.nav.personoversikt.common.utils

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class SelftestGeneratorTest {
    var selftest = SelftestGenerator.getInstance(SelftestGenerator.Config(appname = "testapp", version = "1.0.0"))

    @AfterEach
    internal fun teardown() {
        selftest.clear()
    }

    @Test
    internal fun `should not be ready before every dependency has reported ok`() {
        val reporter = SelftestGenerator.Reporter("dependency", critical = false)

        selftest.assertStatus(isAlive = true, isReady = false)

        reporter.reportOk()

        selftest.assertStatus(isAlive = true, isReady = true)
    }

    @Test
    internal fun `should mark as not alive if critical errors occur`() {
        val nonCritical = SelftestGenerator.Reporter("dependency", critical = false)
        val critical = SelftestGenerator.Reporter("other-dependency", critical = true)

        selftest.assertStatus(isAlive = true, isReady = false)

        nonCritical.reportOk()
        critical.reportOk()

        selftest.assertStatus(isAlive = true, isReady = true)

        nonCritical.reportError(IllegalStateException("Something non-critical is wrong"))
        critical.reportOk()

        selftest.assertStatus(isAlive = true, isReady = true)

        nonCritical.reportOk()
        critical.reportError(IllegalStateException("Something critical is wrong"))

        selftest.assertStatus(isAlive = false, isReady = false)
    }

    @Test
    internal fun `should list all dependencies in selftest`() {
        val nonCritical = SelftestGenerator.Reporter("dependency", critical = false)
        val critical = SelftestGenerator.Reporter("other-dependency", critical = true)

        selftest.assertSelftestContent(
            """
                    Appname: testapp
                    Version: 1.0.0
                    
                    Status:
                    	Name: dependency  Status: Registered
                    	Name: other-dependency (Critical) Status: Registered
            """.trimIndent()
        )

        nonCritical.reportOk()
        critical.reportOk()

        selftest.assertSelftestContent(
            """
                    Appname: testapp
                    Version: 1.0.0
                    
                    Status:
                    	Name: dependency  Status: OK
                    	Name: other-dependency (Critical) Status: OK
            """.trimIndent()
        )

        nonCritical.reportError(IllegalStateException("Non critical error"))
        critical.reportError(IllegalStateException("Critical error"))

        selftest.assertSelftestContent(
            """
                Appname: testapp
                Version: 1.0.0
                
                Status:
                	Name: dependency  Status: KO: Non critical error
                	Name: other-dependency (Critical) Status: KO: Critical error
            """.trimIndent()
        )
    }

    @Test
    fun `should include metadata fields if registered`() {
        SelftestGenerator.Reporter("dependency", critical = false).reportOk()
        SelftestGenerator.Reporter("other-dependency", critical = true).reportOk()
        SelftestGenerator.Metadata("some metadata") {
            "There are 1337 users logged in"
        }

        selftest.assertSelftestContent(
            """
                    Appname: testapp
                    Version: 1.0.0
                    
                    Status:
                    	Name: dependency  Status: OK
                    	Name: other-dependency (Critical) Status: OK
                    
                    Metadata:
                    	Name: some metadata Value: There are 1337 users logged in
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
}
