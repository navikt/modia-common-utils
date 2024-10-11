package no.nav.personoversikt.common.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CompletableFuture

object ConcurrencyUtils {
    fun <FIRST, SECOND> inParallel(
        first: () -> FIRST,
        second: () -> SECOND,
    ): Pair<FIRST, SECOND> {
        val firstTask = CompletableFuture.supplyAsync(first)
        val secondTask = CompletableFuture.supplyAsync(second)

        CompletableFuture.allOf(firstTask, secondTask).get()

        return Pair(firstTask.get(), secondTask.get())
    }

    fun <T> List<() -> T>.runInParallel(): List<T> =
        runBlocking(Dispatchers.IO) {
            this@runInParallel
                .map {
                    async {
                        it()
                    }
                }.awaitAll()
        }
}

fun <TYPE, RETURN> ThreadLocal<TYPE>.withValue(
    value: TYPE,
    block: () -> RETURN,
): RETURN {
    val original = this.get()
    this.set(value)
    val result = block()
    this.set(original)
    return result
}

fun <TYPE, RETURN> ThreadLocal<TYPE>.makeTransferable(block: () -> RETURN): () -> RETURN {
    val current = this.get()
    return { this.withValue(current, block) }
}
