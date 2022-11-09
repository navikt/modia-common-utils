package no.nav.personoversikt.common.science.switcher

import no.nav.personoversikt.common.science.Rate
import org.slf4j.LoggerFactory
import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Proxy

object ProxySwitcher {
    private val log = LoggerFactory.getLogger(ProxySwitcher::class.java)

    @PublishedApi
    internal class ProxyHandler<T : Any>(
        private val name: String,
        private val switch: Rate,
        private val ifEnabled: T,
        private val ifDisabled: T,
    ) : InvocationHandler {
        override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any {
            val nullsafeArgs = args ?: arrayOfNulls<Any>(0)
            return try {
                if (switch.evaluate()) {
                    log.warn("[ProxySwitcher] $name is enabled")
                    method.invoke(ifEnabled, *nullsafeArgs)
                } else {
                    method.invoke(ifDisabled, *nullsafeArgs)
                }
            } catch (e: InvocationTargetException) {
                throw e.targetException
            }
        }
    }

    inline fun <reified T : Any> createSwitcher(
        switch: Rate,
        ifEnabled: T,
        ifDisabled: T,
    ): T {
        val handler = ProxyHandler(
            name = T::class.java.simpleName,
            switch = switch,
            ifEnabled = ifEnabled,
            ifDisabled = ifDisabled
        )

        val proxy = Proxy.newProxyInstance(
            T::class.java.classLoader,
            arrayOf(T::class.java),
            handler
        )

        return proxy as T
    }
}
