package no.nav.personoversikt.test.testenvironment

import org.junit.rules.MethodRule
import org.junit.runners.model.FrameworkMethod
import org.junit.runners.model.Statement

class TestEnvironmentRule(
    testEnvironment: Map<String, String?>
) : MethodRule {

    val runner = TestEnvironment(testEnvironment)

    constructor(vararg pairs: Pair<String, String?>) : this(mapOf(*pairs))

    override fun apply(statement: Statement, method: FrameworkMethod, target: Any): Statement {
        return object : Statement() {
            override fun evaluate() {
                runner.beforeTestEnvironmentRunner()
                statement.evaluate()
                runner.afterTestEnvironmentRunner()
            }
        }
    }
}
