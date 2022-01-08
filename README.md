# 一个支持嵌套的变量处理库(A library supporting nested placeHold)
[![](https://jitpack.io/v/way-zer/PlaceHoldLib.svg)](https://jitpack.io/#way-zer/PlaceHoldLib)
采用kotlin编写,可运行在jvm平台  
Write in kotlin, can use on jvm platform
## 功能(Features)
- [x] 变量替换(text variable)
- [x] 全局变量处理(global variable and provider)
- [x] 嵌套变量处理(nested variable replacement)
- [x] 支持参数,可用于格式化时间日期等(Support)
## 使用(Use)
Step 1. Add the JitPack repository to your build file
```groovy
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
```
Step 2. Add the dependency
```groovy
dependencies {
    implementation 'com.github.way-zer:PlaceHoldLib:Tag'
}
```
## 快速开始(Quick Start)
```kotlin
    import cf.wayzer.placehold.PlaceHoldApi.with
    println("Hello {v}".with("v" to "World")) //OUT: Hello World
    
    
    PlaceHoldApi.registerTypeBinder(Date::class){ DateResolver(it) }
    println("{t}".with("t" to Date(0))) //OUT 01-01
    println("{t:yyyy-MM-dd}".with("t" to Date(0))) //OUT 1970-01-0
    
    
    val v = DynamicResolver.new {
        it?:"NO Params"
    }
    println("{v}".with("v" to v))//OUT: NO Params
    println("{v:}".with("v" to v))//OUT: 
    println("{v:123 456}".with("v" to v))//OUT 123 456
    
    
    data class Data(val a: Int, val b: String)
    PlaceHoldApi.registerGlobalVar("o",object :DynamicVar<Data>(){
        override fun resolveThis(context: PlaceHoldContext): Data? {
            return context.getVar("_o") as? Data
        }
        init {
            registerChild("a"){it?.a}
            registerChild("b"){it?.b}
        }

        override fun getThisSting(context: PlaceHoldContext): String? {
            return resolveThis(context)?.toString()
        }
    })
    println("{o.a} {o.b}".with("_o" to Data(22, "ab")))//OUT: 22 ab
    println("{o.} {o}".with("_o" to Data(22, "ab")))//OUT: {o.} Data(a=22, b=ab)
```
更多例子,请查看[Test.kt](./src/test/kotlin/Test.kt)  
For more example see [Test.kt](./src/test/kotlin/Test.kt)
