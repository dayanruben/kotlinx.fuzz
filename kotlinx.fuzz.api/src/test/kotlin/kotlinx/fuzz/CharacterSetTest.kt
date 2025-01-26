package kotlinx.fuzz

import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class CharacterSetTest {
    @Test
    fun `test character set constructors`() {
        assertDoesNotThrow {
            CharacterSet('a'..'z')
            CharacterSet('a', 'g', 'z')
        }
        assertThrows<IllegalArgumentException> {
            CharacterSet()
        }
        assertThrows<IllegalArgumentException> {
            CharacterSet('a'..'a')
        }
        assertThrows<IllegalArgumentException> {
            CharacterSet('z'..'a')
        }
    }

    @Test
    fun `test character set iterator`() {
        var charset = CharacterSet('a'..'z')
        var expected = ('a'..'z').toList()
        assertEquals(
            expected,
            charset.toList(),
        )

        charset = CharacterSet(
            'a'..'z',
            'A'..'Z',
        )
        expected = expected + ('A'..'Z').toList()
        assertEquals(
            expected,
            charset.toList(),
        )

        charset = CharacterSet(
            linkedSetOf('a'..'z', 'A'..'Z'),
            linkedSetOf('0', '1', '2', '3', 'x'),
        )
        expected = expected + listOf('0', '1', '2', '3', 'x')
        assertEquals(
            expected,
            charset.toList(),
        )

        charset = CharacterSet(
            '0', '1', '2', '3', 'x',
        )
        expected = listOf('0', '1', '2', '3', 'x')
        assertEquals(
            expected,
            charset.toList(),
        )
    }

    @Test
    fun `test size`() {
        var charset = CharacterSet('a'..'z')
        var expected = ('a'..'z').count()
        assertEquals(
            expected,
            charset.size,
        )

        charset = CharacterSet(
            'a'..'z',
            'A'..'Z',
        )
        expected += ('A'..'Z').count()
        assertEquals(
            expected,
            charset.size,
        )

        charset = CharacterSet(
            linkedSetOf('a'..'z', 'A'..'Z'),
            linkedSetOf('0', '1', '2', '3', 'x'),
        )
        expected += linkedSetOf('0', '1', '2', '3', 'x').count()
        assertEquals(
            expected,
            charset.size,
        )
    }
}
