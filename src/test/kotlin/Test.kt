import cf.wayzer.placehold.DynamicVar
import cf.wayzer.placehold.PlaceHoldApi
import cf.wayzer.placehold.PlaceHoldApi.with
import org.junit.Assert
import org.junit.Test
import java.util.*

class Test {
    @Test
    fun base() {
        Assert.assertEquals("Hello World", "Hello World".with().toString())
    }

    @Test
    fun testVars() {
        Assert.assertEquals("Hello World", "Hello {v}".with("v" to "World").toString())
    }

    @Test
    fun nested() {
        val nest1 = "Here will refer nest2: {nest2}".with()
        val nest2 = "Here {nest2} is string".with("nest2" to "nest2")
        Assert.assertEquals("nest Test 'Here will refer nest2: Here nest2 is string' 'Here nest2 is string'",
                "nest Test '{nest1}' '{nest2}'".with("nest1" to nest1, "nest2" to nest2).toString())
    }

    @Test
    fun nestedVar() {
        PlaceHoldApi.registerGlobalVar("nest3", "nested {v}".with())
        PlaceHoldApi.registerGlobalDynamicVar("nestVar"){_,_-> getVar("nest3")}
        Assert.assertEquals("nested Var", "{nestVar}".with("v" to "Var").toString())
    }

    data class Data(val a: Int, val b: String)


    @Test
    fun badPath() {
        Assert.assertEquals("{o.d} {o.f}", "{o.d} {o.f}".with("o" to Data(22, "ab")).toString())
    }

    @Test
    fun testParams() {
        val v = DynamicVar { _, _: Any, params -> params ?: "NO Params" }
        Assert.assertEquals("NO Params", "{v}".with("v" to v).toString())
        Assert.assertEquals("", "{v:}".with("v" to v).toString())
        Assert.assertEquals("123 456", "{v:123 456}".with("v" to v).toString())
    }

    @Test
    fun testTypeBind() {
        PlaceHoldApi.typeBinder<Data>().apply {
            registerChild("a", DynamicVar.obj { it.a })
            registerChild("b", DynamicVar.obj { it.b })
            registerToString { _, obj, _ -> obj.toString() }
        }
        Assert.assertEquals("22 ab", "{o.a} {o.b}".with("o" to Data(22, "ab")).toString())
        Assert.assertEquals("{o.} Data(a=22, b=ab)", "{o.} {o}".with("o" to Data(22, "ab")).toString())
    }

    @Test
    fun testDateTypeBinder() {
        Assert.assertEquals("01-01", "{t}".with("t" to Date(0)).toString())
        Assert.assertEquals("1970-01-01", "{t:yyyy-MM-dd}".with("t" to Date(0)).toString())
    }

    @Test
    fun testGlobalContext() {
        Assert.assertEquals("01-01", PlaceHoldApi.GlobalContext.typeResolve(Date(0)).toString())
    }

    @Test
    fun testCache() {
        PlaceHoldApi.typeBinder<Data>().apply {
            registerChild("a", DynamicVar.obj { it.a })
            registerChild("b", DynamicVar.obj { it.b })
            registerToString { _, obj, _ -> obj.toString() }
        }
        var count = 0
        val a = DynamicVar.v { count++;Data(22, "ab") }
        "{o.a} {o.b} {o} {o.a}".with("o" to a).toString()
        Assert.assertEquals(1, count)
    }
}
