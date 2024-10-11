package no.nav.personoversikt.common.science.scientist

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest

class ScientistFilter : Filter {
    override fun doFilter(
        request: ServletRequest,
        response: ServletResponse,
        chain: FilterChain,
    ) {
        val req = request as HttpServletRequest
        if (req.queryString?.contains("forceExperiment") == true) {
            Scientist.forceExperiment.set(true)
        }
        chain.doFilter(request, response)
        Scientist.forceExperiment.remove()
    }
}
