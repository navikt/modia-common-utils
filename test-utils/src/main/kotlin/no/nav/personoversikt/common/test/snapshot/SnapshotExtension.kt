package no.nav.personoversikt.common.test.snapshot

import no.nav.personoversikt.common.test.snapshot.format.JsonSnapshotFormat
import org.junit.jupiter.api.extension.*

open class SnapshotExtension(
    path: String = "src/test/resources/snapshots",
    format: SnapshotRunner.Fileformat = JsonSnapshotFormat,
    debug: Boolean = false,
    private val runner: SnapshotRunnerImpl = SnapshotRunnerImpl(path, format, debug)
) : ParameterResolver,
    BeforeTestExecutionCallback,
    AfterTestExecutionCallback,
    SnapshotRunner by runner {

    override fun beforeTestExecution(context: ExtensionContext) {
        val className = context.testClass.get().name
        val methodName = context.testMethod.get().name.replace("\$modiabrukerdialog_web", "")
        runner.beforeSnapshotRunner("${className}_$methodName")
    }

    override fun afterTestExecution(context: ExtensionContext) {
        runner.afterSnapshotRunner()
    }

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
        return parameterContext.parameter.type == SnapshotExtension::class.java
    }

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any {
        return this
    }
}
