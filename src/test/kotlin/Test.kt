import cf.wayzer.placehold.DynamicVar
import cf.wayzer.placehold.PlaceHoldApi
import cf.wayzer.placehold.PlaceHoldApi.with
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.assertThrows
import java.util.*

class Test {
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
        PlaceHoldApi.registerGlobalDynamicVar("nestVar") { _, _ -> getVar("nest3") }
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
        Assertions.assertEquals("{o.d} {o.f}", "{o.d} {o.f}".with("o" to Data(22, "ab")).toString())
    }

    @Test
    fun testParams() {
        val v = DynamicVar { _, _: Any, params -> params ?: "NO Params" }
        Assertions.assertEquals("NO Params", "{v}".with("v" to v).toString())
        Assertions.assertEquals("", "{v:}".with("v" to v).toString())
        Assertions.assertEquals("123 456", "{v:123 456}".with("v" to v).toString())
    }

    @Test
    fun testTypeBind() {
        PlaceHoldApi.typeBinder<Data>().apply {
            registerChild("a", DynamicVar.obj { it.a })
            registerChild("b", DynamicVar.obj { it.b })
            registerToString { _, obj, _ -> obj.toString() }
        }
        Assertions.assertEquals("22 ab", "{o.a} {o.b}".with("o" to Data(22, "ab")).toString())
        Assertions.assertEquals("{o.} Data(a=22, b=ab)", "{o.} {o}".with("o" to Data(22, "ab")).toString())
    }

    @Test
    fun testDateTypeBinder() {
        Assertions.assertEquals("01-01", "{t}".with("t" to Date(0)).toString())
        Assertions.assertEquals("1970-01-01", "{t:yyyy-MM-dd}".with("t" to Date(0)).toString())
    }

    @Test
    fun testGlobalContext() {
        Assertions.assertEquals("01-01", PlaceHoldApi.GlobalContext.resolveVar(Date(0)))
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

    @Test()
    fun testAllKeySet() {
        assertThrows<IllegalStateException> {
            PlaceHoldApi.registerGlobalVar("a.b.c.*", 0)
        }
    }

    @Test
    fun testAllKeyGet() {
        PlaceHoldApi.registerGlobalVar("list.2", 2)
        PlaceHoldApi.registerGlobalVar("list.3", DynamicVar.v { "DD" })
        Assertions.assertEquals("0,1,2,DD", "{list.*}".with("list.0" to 0, "list.1" to 1).toString())
    }

    @Test
    fun testTypeAllKeyGet() {
        PlaceHoldApi.typeBinder<Data>().apply {
            registerChild("list.1", DynamicVar.obj { it.a })
            registerChild("list.2", DynamicVar.obj { it.b })
        }
        Assertions.assertEquals("8,10", "{o.list.*}".with("o" to Data(8, "10")).toString())
    }

    @Test
    fun testFunctionVar() {
        PlaceHoldApi.registerGlobalVar("upperCase", DynamicVar.params {
            if (it == null) return@params "{upperCase:NoParam}"
            getVarString(it)?.uppercase()
        })
        Assertions.assertEquals("{upperCase:NoParam}", "{upperCase}".with().toString())
        Assertions.assertEquals("UPPER", "{upperCase:a}".with("a" to "upper").toString())
    }

    @Test
    fun testLocalOverlay() {
        PlaceHoldApi.registerGlobalVar("v", "global")
        Assertions.assertEquals("local", "{v}".with("v" to "local").toString())
    }

    @Test
    fun testVarTreeAndDynamic() {
        Assertions.assertEquals(
            "",
            "{v.*}".with("v.a" to DynamicVar.v { null }).toString()
        )
    }

    @Test
    fun testCacheBug() {
        val sub = "{a}".with()
        Assertions.assertEquals("a", "{sub}".with("sub" to sub, "a" to "a").toString())
        Assertions.assertEquals("b", "{sub}".with("sub" to sub, "a" to "b").toString())
    }

    @Test
    @Disabled //TODO fix this
    fun testCacheBug2() {
        val sub = "{a}".with("a" to "a")
        Assertions.assertEquals("ba", "{a}{sub}".with("sub" to sub, "a" to "b").toString())
    }
}
