import cf.wayzer.placehold.*
import cf.wayzer.placehold.PlaceHoldApi.registerGlobalVar
import cf.wayzer.placehold.PlaceHoldApi.with
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.assertThrows
import java.io.File
import java.lang.IllegalArgumentException
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
        registerGlobalVar("nest3", "nested {v}".with())
        PlaceHoldApi.registerGlobalDynamicVar("nestVar") { _, _ -> VarToken("nest3").get() }
        Assertions.assertEquals("nested Var", "{nestVar}".with("v" to "Var").toString())
    }

    @Test
    fun testList() {
        val list = listOf(0, 1, 2, 3, 4, 5)
        Assertions.assertEquals("0,1,2,3,4,5", "{list}".with("list" to list).toString())
        Assertions.assertEquals("0", "{list.first}".with("list" to list).toString())
        Assertions.assertEquals("3", "{list.3}".with("list" to list).toString())
        Assertions.assertEquals("5", "{list.last}".with("list" to list).toString())
    }


    data class Data(val a: Int, val b: String) : VarContainer {
        override fun resolve(ctx: VarString, child: String): Any? {
            if (child == "testVarContainer") return "YES"
            return null
        }
    }

    @Test
    fun badPath() {
        Assertions.assertEquals("{ERR: not found o.d} YES", "{o.d} {o.testVarContainer}".with("o" to Data(22, "ab")).toString())
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
        //移除绑定
        PlaceHoldApi.typeBinder<Data>().apply {
            registerChildAny("a", null)
        }
        Assertions.assertEquals("{ERR: not found o.a} ab", "{o.a} {o.b}".with("o" to Data(22, "ab")).toString())
    }

    @Test
    fun testDateTypeBinder() {
        Assertions.assertEquals("01-01", "{t}".with("t" to Date(0)).toString())
        Assertions.assertEquals("1970-01-01", """{t "yyyy-MM-dd"}""".with("t" to Date(0)).toString())
    }

    @Test
    fun testGlobalContext() {
        Assertions.assertEquals("01-01", PlaceHoldApi.GlobalContext.resolveVarForString(Date(0)))
    }

    @Test
    fun testTemplateHandler() {
        Assertions.assertEquals("b", "a".with(TemplateHandlerKey to TemplateHandler {
            Assertions.assertEquals("a", it)
            "b"
        }).toString())
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
    fun testFunctionVar() {
        registerGlobalVar("upperCase", DynamicVar.params {
            it.get<String>(0).uppercase()
        })
        Assertions.assertEquals("{ERR: Fail resolve upperCase: Parma 0 required type String}", "{upperCase}".with().toString())
        Assertions.assertEquals("UPPER", "{upperCase a}".with("a" to "upper").toString())
    }

    @Test
    fun testLocalOverlay() {
        registerGlobalVar("v", "global")
        Assertions.assertEquals("local", "{v}".with("v" to "local").toString())
    }

    @Test
    fun testCacheBug() {
        val sub1 = "{a}".with()
        Assertions.assertEquals("a", "{sub}".with("sub" to sub1, "a" to "a").toString())
        Assertions.assertEquals("b", "{sub}".with("sub" to sub1, "a" to "b").toString())
        val sub2 = "{a}".with("a" to "a")
        Assertions.assertEquals("ba", "{a}{sub}".with("sub" to sub2, "a" to "b").toString())
    }

    interface CustomBuilderMessage

    @Test
    fun testCustomBuilder() {
        registerGlobalVar("image", DynamicVar.v { "{ERR: receiver not support image}" })
        val text = "这是文字{image imageFile}这是文字".with("imageFile" to File("image.png"))
        Assertions.assertEquals("这是文字{ERR: receiver not support image}这是文字", text.toString())


        data class Text(val value: String) : CustomBuilderMessage
        data class Image(val value: String) : CustomBuilderMessage

        val builderCtx = VarString(
            "", mapOf(
                "image" to DynamicVar.params {
                    Image("{OUTPUT IMAGE ${it.get<File>(0)}}")
                },
            ), cache = null
        )
        val resolved = builderCtx.createChild(text.text, text.vars).parsed().map { token ->
            if (token is String) Text(token) else (token as VarString.VarToken).let {
                val obj = it.get()
                if (obj is CustomBuilderMessage) return@map obj
                Text(it.getForString(obj))
            }
        }
        Assertions.assertEquals(3, resolved.size)
        Assertions.assertTrue(resolved[0] is Text)
        Assertions.assertTrue(resolved[1] is Image)
        Assertions.assertTrue(resolved[2] is Text)
    }

    @Test
    fun testStdVariable() {
        Assertions.assertEquals("0,1,2", "{list|join}".with("list" to listOf(0, 1, 2)).toString())
        Assertions.assertEquals("<0>|<1>|<2>", """{join list "|" "<{it}>"}""".with("list" to listOf(0, 1, 2)).toString())

        registerGlobalVar("list.2", 2)
        registerGlobalVar("list.3", DynamicVar.v { "DD" })
        registerGlobalVar("list.null", DynamicVar.v { null })
        Assertions.assertEquals("0,1,2,DD", "{listPrefix list}".with("list.0" to 0, "list.1" to 1).toString())


        //Not support now
//        PlaceHoldApi.typeBinder<Data>().apply {
//            registerChild("list.1", DynamicVar.obj { it.a })
//            registerChild("list.2", DynamicVar.obj { it.b })
//        }
//        Assertions.assertEquals("8,10", "{listPrefix o.list}".with("o" to Data(8, "10")).toString())

        Assertions.assertEquals("NO", """{v|or "NO"}""".with().toString())
        Assertions.assertEquals("YES", """{v|or "NO"}""".with("v" to "YES").toString())
    }
}
