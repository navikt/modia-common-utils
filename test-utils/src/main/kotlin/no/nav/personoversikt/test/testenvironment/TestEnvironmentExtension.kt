package no.nav.personoversikt.test.testenvironment

import org.junit.jupiter.api.extension.*

class TestEnvironmentExtension(
    testEnvironment: Map<String, String?> = emptyMap(),
    private val runner: TestEnvironmentRunnerImpl = TestEnvironmentRunnerImpl(testEnvironment)
) : BeforeTestExecutionCallback,
    AfterTestExecutionCallback,
    ParameterResolver,
    TestEnvironmentRunner by TestEnvironmentRunnerImpl(testEnvironment) {
    constructor(vararg pairs: Pair<String, String?>) : this(mapOf(*pairs))

    override fun beforeTestExecution(context: ExtensionContext) {
        runner.beforeTestEnvironmentRunner()
    }

    override fun afterTestExecution(context: ExtensionContext) {
        runner.afterTestEnvironmentRunner()
    }

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
        return parameterContext.parameter.type == TestEnvironmentExtension::class.java
    }

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any {
        return this
    }
}
