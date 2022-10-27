package no.nav.personoversikt.test.testenvironment

class TestEnvironment(private val testEnvironment: Map<String, String?>) {
    private val originalEnvironment: Map<String, String?> = testEnvironment.keys.associateWith(System::getProperty)

    fun beforeTestEnvironmentRunner() {
        setAsEnvironment(testEnvironment)
    }

    fun afterTestEnvironmentRunner() {
        setAsEnvironment(originalEnvironment)
    }

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
