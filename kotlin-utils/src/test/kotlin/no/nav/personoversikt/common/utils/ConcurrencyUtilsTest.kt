package no.nav.personoversikt.common.utils

import no.nav.personoversikt.common.utils.ConcurrencyUtils.runInParallel
import no.nav.personoversikt.common.utils.KotlinUtils.plusminus
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.system.measureTimeMillis

internal class ConcurrencyUtilsTest {
    private val slowtask = {
        Thread.sleep(1000)
        "ok"
    }

    @Test
    internal fun `runs tasks in parallel`() {
        val time = measureTimeMillis { ConcurrencyUtils.inParallel(slowtask, slowtask) }

        assertTrue(time in (1000 plusminus 200))
    }

    @Test
    internal fun `runs all tasks in parallel`() {
        val tasks = listOf(slowtask, slowtask, slowtask, slowtask, slowtask)
        val time =
            measureTimeMillis {
                tasks.runInParallel()
            }
        assertTrue(time in (1000 plusminus 200))
    }

    @Test
    internal fun `run blocks of code with threadlocal value`() {
        val threadlocal = ThreadLocal<String>().also { it.set("original") }
        val result =
            threadlocal.withValue("other") {
                threadlocal.get()
            }
        assertEquals("original", threadlocal.get())
        assertEquals("other", result)
    }

    @Test
    internal fun `makes function copy threadlocal before running`() {
        val threadlocal = ThreadLocal<String>().also { it.set("original") }
        val task = { threadlocal.get() }

        val (original, withTransfer) =
            ConcurrencyUtils.inParallel(
                task,
                threadlocal.makeTransferable(task),
            )

        assertNull(original)
        assertEquals("original", withTransfer)
    }
}
