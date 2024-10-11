package no.nav.personoversikt.common.test.testenvironment

import org.junit.jupiter.api.extension.*

class TestEnvironmentExtension(
    testEnvironment: () -> Map<String, String?>,
) : BeforeTestExecutionCallback,
    AfterTestExecutionCallback,
    ParameterResolver {
    private val testEnvironment: TestEnvironment = TestEnvironment(testEnvironment)

    constructor(vararg pairs: Pair<String, String?>) : this(mapOf(*pairs))
    constructor(testEnvironment: Map<String, String?>) : this({ testEnvironment })

    override fun beforeTestExecution(context: ExtensionContext) {
        testEnvironment.beforeTestExecution()
    }

    override fun afterTestExecution(context: ExtensionContext) {
        testEnvironment.afterTestExecution()
    }

    override fun supportsParameter(
        parameterContext: ParameterContext,
        extensionContext: ExtensionContext,
    ): Boolean = parameterContext.parameter.type == TestEnvironmentExtension::class.java

    override fun resolveParameter(
        parameterContext: ParameterContext,
        extensionContext: ExtensionContext,
    ): Any = this
}
