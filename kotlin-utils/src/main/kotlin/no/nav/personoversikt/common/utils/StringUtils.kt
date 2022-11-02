package no.nav.personoversikt.common.utils

object StringUtils {
    fun String.isNumeric(): Boolean = this.all { it.isDigit() }
    fun String.isLetters(): Boolean = this.all { it.isLetter() }

    fun String.cutoff(size: Int): String {
        return if (this.length <= size) {
            this
        } else {
            this.substring(0, size - 3) + "..."
        }
    }

    fun CharSequence.indicesOf(other: String, startIndex: Int = 0, ignoreCase: Boolean = false): List<Int> {
        val indices = mutableListOf<Int>()
        var index = this.indexOf(other, startIndex, ignoreCase)
        while (index != -1) {
            indices.add(index)
            index = this.indexOf(other, index + 1, ignoreCase)
        }
        return indices
    }

    fun String.removePrefix(prefix: CharSequence, ignoreCase: Boolean = false): String {
        if (this.startsWith(prefix, ignoreCase)) {
            return this.substring(prefix.length)
        }
        return this
    }

    fun String.addPrefixIfMissing(prefix: String, ignoreCase: Boolean = false): String {
        return if (this.startsWith(prefix, ignoreCase)) {
            this
        } else {
            "$prefix$this"
        }
    }
}
