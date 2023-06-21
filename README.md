# 一个支持嵌套的变量处理库(A library supporting nested placeHold)

[![](https://jitpack.io/v/way-zer/PlaceHoldLib.svg)](https://jitpack.io/#way-zer/PlaceHoldLib)
采用kotlin编写,可运行在jvm平台  
Write in kotlin, can use on jvm platform

## 功能(Features)

- [x] 变量替换(text variable)
- [x] 全局变量处理(global variable and provider)
- [x] 嵌套变量处理(nested variable replacement)
- [x] 支持参数,可用于格式化时间日期等(Support)

## 规范(Spec)
### 变量格式 Variable Format
* 基础格式 Base format: `{xxx}`
* 变量类型 Types: `var` `"substring"`
* 转义 escape: `{{`->`{`, only in substring`\"`->`"`
* 函数调用 Function: `{f a "b"}` -> Like `f(a,"b")`
  * 使用任何空白字符分隔 split by any black: (like ' ' or '\n')
* 链式调用 Pipe invoke: `{f a "b"|f2 c}` -> Like `f2(f(a,"b"),c)`
  * 使用'|'分隔，前面内容将作为第一个参数传递 split by '|', result as the first parameter
* 命名约束 Name Rule: name of variable `[^{}|\s]+`; inside const ""
### 变量解析-变量模式 Variable resolve - Variable mode
* 变量未找到，返回null variable notFound: null
* 普通变量，返回原始值`unwarp`
* 动态变量，递归`unwarp`返回
* 下面用`a->b`表示解析对象`a`的子变量`b`
* `a.b`，如果存在`a.b`直接返回，否则尝试解析 a->b
* `a.b.c`：依次查找`a.b.c` `a.b->c` `a->b->c`
### 变量解析-文本模式 Variable resolve - String mode
* 先根据变量模式解析
* 如果变量不存在，保持原样
* 尝试解析`x->toString`,否则调用Java的ToString函数
## 使用(Use)

Step 1. Add the JitPack repository to your build file

```groovy
allprojects{
    repositories{
        maven{ url 'https://jitpack.io' }
    }
}
```

Step 2. Add the dependency

```groovy
dependencies{
    implementation 'com.github.way-zer:PlaceHoldLib:Tag'
}
```

## 快速开始(Quick Start)

```kotlin
    import cf.wayzer.placehold.PlaceHoldApi.with

println("Hello {v}".with("v" to "World")) //OUT: Hello World


PlaceHoldApi.registerTypeBinder(Date::class) { DateResolver(it) }
println("{t}".with("t" to Date(0))) //OUT 01-01
println("{t:yyyy-MM-dd}".with("t" to Date(0))) //OUT 1970-01-0


val v = DynamicResolver.new {
    it ?: "NO Params"
}
println("{v}".with("v" to v))//OUT: NO Params
println("{v:}".with("v" to v))//OUT: 
println("{v:123 456}".with("v" to v))//OUT 123 456


data class Data(val a: Int, val b: String)
PlaceHoldApi.registerGlobalVar("o", object : DynamicVar<Data>() {
    override fun resolveThis(context: PlaceHoldContext): Data? {
        return context.getVar("_o") as? Data
    }

    init {
        registerChild("a") { it?.a }
        registerChild("b") { it?.b }
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
