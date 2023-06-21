import cf.wayzer.placehold.DynamicVar
import cf.wayzer.placehold.PlaceHoldApi
import cf.wayzer.placehold.PlaceHoldApi.with
import cf.wayzer.placehold.VarString
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.assertThrows
import java.util.*

class Test {
    @BeforeEach
    fun reset() {
        PlaceHoldApi.init()
    }

    @Test
    fun base() {
        Assertions.assertEquals("Hello World", "Hello World".with().toString())
    }

    @Test
    fun testVars() {
        Assertions.assertEquals("Hello World", "Hello {v}".with("v" to "World").toString())
    }

    @Test
    fun nested() {
        val nest1 = "Here will refer nest2: {nest2}".with()
        val nest2 = "Here {nest2} is string".with("nest2" to "nest2")
        Assertions.assertEquals(
            "nest Test 'Here will refer nest2: Here nest2 is string' 'Here nest2 is string'",
            "nest Test '{nest1}' '{nest2}'".with("nest1" to nest1, "nest2" to nest2).toString()
        )
    }

    @Test
    fun nestedVar() {
        PlaceHoldApi.registerGlobalVar("nest3", "nested {v}".with())
        PlaceHoldApi.registerGlobalDynamicVar("nestVar") { _, _ -> VarToken("nest3").get() }
        Assertions.assertEquals("nested Var", "{nestVar}".with("v" to "Var").toString())
    }

    @Test
    fun testList() {
        PlaceHoldApi//ensure init
        val list = listOf(1, 2, 3, 4, 5)
        Assertions.assertEquals("1,2,3,4,5", "{list}".with("list" to list).toString())
    }


    data class Data(val a: Int, val b: String)

    @Test
    fun badPath() {
        Assertions.assertEquals("{ERR: not found o.d} {ERR: not found o.f}", "{o.d} {o.f}".with("o" to Data(22, "ab")).toString())
    }

    @Test
    fun testTypeBind() {
        PlaceHoldApi.typeBinder<Data>().apply {
            registerChild("a", DynamicVar.obj { it.a })
            registerChild("b", DynamicVar.obj { it.b })
            registerToString { _, obj, _ -> obj.toString() }
        }
        Assertions.assertEquals("22 ab", "{o.a} {o.b}".with("o" to Data(22, "ab")).toString())
        Assertions.assertEquals("{ERR: not found o.} Data(a=22, b=ab)", "{o.} {o}".with("o" to Data(22, "ab")).toString())
    }

    @Test
    fun testDateTypeBinder() {
        Assertions.assertEquals("01-01", "{t}".with("t" to Date(0)).toString())
        Assertions.assertEquals("1970-01-01", "{t \"yyyy-MM-dd\"}".with("t" to Date(0)).toString())
    }

    @Test
    fun testGlobalContext() {
        Assertions.assertEquals("01-01", PlaceHoldApi.GlobalContext.resolveVarForString(Date(0)))
    }

    @Test
    fun testCache() {
        PlaceHoldApi.typeBinder<Data>().apply {
            registerChild("a", DynamicVar.obj { it.a })
            registerChild("b", DynamicVar.obj { it.b })
            registerToString { _, obj, _ -> obj.toString() }
        }
        var count = 0
        val a = DynamicVar.v {
            count++;Data(22, "ab")
        }
        "{o.a} {o.b} {o} {o.a}".with("o" to a).toString()
        Assertions.assertEquals(1, count)
    }

    @Test
    fun testAllKeyGet() {
        PlaceHoldApi.registerGlobalVar("list.2", 2)
        PlaceHoldApi.registerGlobalVar("list.3", DynamicVar.v { "DD" })
        PlaceHoldApi.registerGlobalVar("list.null", DynamicVar.v { null })
        Assertions.assertEquals("0,1,2,DD", "{listPrefix list}".with("list.0" to 0, "list.1" to 1).toString())
    }

    @Test
    @Disabled //Not support now
    fun testTypeAllKeyGet() {
        PlaceHoldApi.typeBinder<Data>().apply {
            registerChild("list.1", DynamicVar.obj { it.a })
            registerChild("list.2", DynamicVar.obj { it.b })
        }
        Assertions.assertEquals("8,10", "{listPrefix o.list}".with("o" to Data(8, "10")).toString())
    }

    @Test
    fun testFunctionVar() {
        PlaceHoldApi.registerGlobalVar("upperCase", DynamicVar.params {
            it.get<String>(0).uppercase()
        })
        Assertions.assertEquals("{ERR: Fail resolve upperCase: Parma 0 required type String}", "{upperCase}".with().toString())
        Assertions.assertEquals("UPPER", "{upperCase a}".with("a" to "upper").toString())
    }

    @Test
    fun testLocalOverlay() {
        PlaceHoldApi.registerGlobalVar("v", "global")
        Assertions.assertEquals("local", "{v}".with("v" to "local").toString())
    }

    @Test
    fun testCacheBug1() {
        val sub = "{a}".with()
        Assertions.assertEquals("a", "{sub}".with("sub" to sub, "a" to "a").toString())
        Assertions.assertEquals("b", "{sub}".with("sub" to sub, "a" to "b").toString())
    }

    @Test
    fun testCacheBug2() {
        val sub = "{a}".with("a" to "a")
        Assertions.assertEquals("ba", "{a}{sub}".with("sub" to sub, "a" to "b").toString())
    }
}
