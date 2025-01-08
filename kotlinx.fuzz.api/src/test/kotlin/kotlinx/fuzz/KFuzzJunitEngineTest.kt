package kotlinx.fuzz

object KFuzzJunitEngineTest {
    @KFuzzTest
    fun foo() {
        println("Hi from MyEngineTest::foo")
    }
}