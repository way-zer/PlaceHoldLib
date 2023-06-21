import cf.wayzer.placehold.TokenParser
import cf.wayzer.placehold.TokenParser.Var
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.Exception
import kotlin.system.measureTimeMillis

class TestParser {
    @Test
    fun readTokens() {
        val sample = """a   b "c \" d" "e" | f } end"""
        val tokens = mutableListOf<Any>()
        val parsed = TokenParser.readTokensTest(sample, tokens)
        Assertions.assertEquals(sample.indexOf('}'), parsed)
        Assertions.assertIterableEquals(listOf(Var("a"), Var("b"), "c \" d", "e", TokenParser.PipeToken, Var("f")), tokens)
    }

    @Test
    fun noEnd() {
        assertThrows<IllegalStateException> { TokenParser.readTokensTest("a \"b c", mutableListOf()) }
        assertThrows<IllegalStateException> { TokenParser.readTokensTest("a \"b\\\" c", mutableListOf()) }
        assertThrows<Exception> { TokenParser.parse("start { no end") }
    }

    @Test
    fun parse() {
        val sample = """> }{{ < start {a   b "c \" d" e | f } end"""
        val parsed = TokenParser.parse(sample)
        Assertions.assertEquals(3, parsed.size)
        Assertions.assertEquals("> }{ < start ", parsed[0].asString)
        Assertions.assertEquals(" end", parsed[2].asString)
    }

    @Test()
    @Disabled
    fun performanceTest() {
        val sample = """> }{{ < start {a   b "c \" d" e | f } end"""
        repeat(100000) {
            TokenParser.parse(sample)
        }
        println(measureTimeMillis {
            repeat(100000) {
                TokenParser.parse(sample)
            }
        })
    }
}
