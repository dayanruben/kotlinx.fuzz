package kotlinx.fuzz

import com.github.curiousoddman.rgxgen.model.RgxGenCharsDefinition
import com.github.curiousoddman.rgxgen.model.SymbolRange
import com.github.curiousoddman.rgxgen.util.chars.CharList

/**
 * Class that represents a custom character set consisting of a list of char ranges and a set of unique symbols
 *
 * @param ranges char ranges that are contained in the character set
 * @param symbols unique characters that are contained in the character set
 */
data class CharacterSet(
    val ranges: Set<CharRange> = emptySet(),
    val symbols: Set<Char> = emptySet(),
) : Iterable<Char> {
    val size = ranges.sumOf { it.last - it.first + 1 } + symbols.size

    init {
        require(size > 0) { "Can't create an empty character set" }
        require(ranges.all { it.step == 1 && (it.start < it.endInclusive) }) {
            "All ranges must be ascending and with step 1"
        }
    }

    internal fun toRgxGenProperties(): RgxGenCharsDefinition = RgxGenCharsDefinition.of(
        ranges.map { SymbolRange.range(it.first.code, it.last.code) },
        CharList.charList(symbols.joinToString("")),
    )

    operator fun contains(char: Char): Boolean = char in symbols || ranges.any { char in it }

    override fun iterator(): Iterator<Char> = It()

    private inner class It : Iterator<Char> {
        private var reachedSymbols = false
        private var currentRange: Iterator<CharRange> = ranges.iterator()
        private var currentSymbol: Iterator<Char> = when {
            currentRange.hasNext() -> currentRange.next().iterator()
            else -> {
                reachedSymbols = true
                symbols.iterator()
            }
        }

        init {
            step()
        }

        private fun step() {
            while (!currentSymbol.hasNext()) {
                when {
                    reachedSymbols -> return
                    currentRange.hasNext() -> currentSymbol = currentRange.next().iterator()
                    else -> {
                        reachedSymbols = true
                        currentSymbol = symbols.iterator()
                    }
                }
            }
        }

        override fun hasNext(): Boolean = currentSymbol.hasNext()

        override fun next(): Char = currentSymbol.next().also {
            step()
        }
    }

    companion object {
        val US_LETTERS = CharacterSet(setOf('a'..'z', 'A'..'Z'))
        val WHITESPACES = CharacterSet(setOf(' ', '\t', '\r', '\n', '\u000B'))
    }
}

fun CharacterSet(vararg ranges: CharRange): CharacterSet = CharacterSet(ranges = ranges.toSet())
fun CharacterSet(symbols: Set<Char>): CharacterSet = CharacterSet(ranges = emptySet(), symbols = symbols)
fun CharacterSet(vararg symbols: Char): CharacterSet = CharacterSet(ranges = emptySet(), symbols = symbols.toSet())
