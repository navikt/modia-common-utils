package no.nav.personoversikt.test.testenvironment

interface TestEnvironmentRunner {
    fun runWithEnvironment(environment: Map<String, String?>, block: () -> Unit)
    companion object {
        fun runWithEnvironment(environment: Map<String, String?>, block: () -> Unit) {
            val original = environment.keys.associateWith(System::getProperty)
            setAsEnvironment(environment)
            block()
            setAsEnvironment(original)
        }

        fun setAsEnvironment(environment: Map<String, String?>) {
            environment.forEach { (key, value) ->
                if (value == null) {
                    System.clearProperty(key)
                } else {
                    System.setProperty(key, value)
                }
            }
        }
    }
}

class TestEnvironmentRunnerImpl(
    private val testEnvironment: Map<String, String?>
) : TestEnvironmentRunner {
    private val originalEnvironment: Map<String, String?> = testEnvironment.keys.associateWith(System::getProperty)

    fun beforeTestEnvironmentRunner() {
        TestEnvironmentRunner.setAsEnvironment(testEnvironment)
    }

    fun afterTestEnvironmentRunner() {
        TestEnvironmentRunner.setAsEnvironment(originalEnvironment)
    }

    override fun runWithEnvironment(environment: Map<String, String?>, block: () -> Unit) = TestEnvironmentRunner.runWithEnvironment(environment, block)
}
