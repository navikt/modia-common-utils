package no.nav.personoversikt.test.testenvironment

class TestEnvironment(private val testEnvironment: Map<String, String?>) {
    private var originalEnvironment: Map<String, String?>? = null

    internal fun beforeTestExecution() {
        originalEnvironment = testEnvironment.keys.associateWith(System::getProperty)
        setAsEnvironment(testEnvironment)
    }

    internal fun afterTestExecution() {
        val environment = requireNotNull(originalEnvironment) {
            "Missing original environment, ensure `setupTestEnvironment` has been called"
        }
        setAsEnvironment(environment)
    }

    companion object {
        fun withEnvironment(environment: Map<String, String?>, block: () -> Unit) {
            TestEnvironment(environment).run {
                beforeTestExecution()
                block()
                afterTestExecution()
            }
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
