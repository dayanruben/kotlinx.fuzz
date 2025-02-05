package kotlinx.fuzz.test

object RealUserCode {
    fun method1(a: Int, b: Int, c: Int, d: Boolean) {
        if (a % 2 == 0 && b % 3 == 2 && c % 31 == 11 && d) {
            listOf("this is happening")
        }
    }

}