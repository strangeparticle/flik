import kotlin.test.Test
import kotlin.test.assertEquals

class GreetingTest {
    @Test
    fun greetsTheGivenName() {
        assertEquals("Hello, world!", greeting("world"))
    }

    @Test
    fun greetsACustomName() {
        assertEquals("Hello, Ada!", greeting("Ada"))
    }
}
