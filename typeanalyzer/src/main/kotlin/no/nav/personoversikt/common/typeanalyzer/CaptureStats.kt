package no.nav.personoversikt.common.typeanalyzer

class CaptureStats {
    var count: Int = 0
        private set

    var countSizeLastChange: Int = 0
        private set

    var errors: Int = 0
        private set

    var lastException: Throwable? = null

    val confidence: Double
        get() = countSizeLastChange.toDouble() / count

    fun capture(changed: Boolean) {
        count++
        if (changed) {
            countSizeLastChange = 0
        } else {
            countSizeLastChange++
        }
    }

    fun exception(exception: Throwable) {
        errors++
        lastException = exception
    }
}
