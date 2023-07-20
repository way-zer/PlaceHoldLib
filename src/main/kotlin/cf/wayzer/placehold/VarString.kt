package cf.wayzer.placehold

import java.util.*

/**
 * 新变量解析流程设计
 * 1. 尝试根据2在当前Context解析，不存在则在parent解析
 * 2. 优先根据最大前缀，依次查找，unwrap后作为obj，尝试解析子变量`a->b`，直到终端节点。存入缓存
 * 3. 对终端节点传递params求解。若有管道，保存成临时变量，作为第一参数，解析管道
 * 4. 如果forString,对最终结果使用toString求解，空params
 */

@Suppress("MemberVisibilityCanBePrivate", "unused")
data class VarString(
    val text: String,
    val vars: Map<String, Any?>,
    private val parent: VarString? = null,
    val cache: MutableMap<List<String>, Any>? = mutableMapOf()
) : VarType {
    fun parent() = parent ?: globalContext.takeIf { it != this }

    @JvmInline
    value class Parameters(val params: List<Any>) {
        constructor(vararg params: Any) : this(params.toList())

        @Throws(IllegalArgumentException::class)
        inline fun <reified T : Any> getOrNull(i: Int, default: T? = null): T? {
            var arg = params.getOrNull(i) ?: default
            if ((arg is VarString || arg is VarToken) && T::class.java == String::class.java)
                arg = if (arg is VarToken) arg.getForString() else arg.toString()
            if (arg is VarToken && T::class.java != VarToken::class.java) arg = arg.get()
            require(arg is T?) { "Parma $i required type ${T::class.java.simpleName}, get $arg" }
            return arg
        }

        @Throws(IllegalArgumentException::class)
        inline fun <reified T : Any> get(i: Int, default: T? = null): T {
            return getOrNull<T>(i, default)
                ?: throw IllegalArgumentException("Parma $i required type ${T::class.java.simpleName}")
        }

        companion object {
            val Empty = Parameters(emptyList())
        }
    }

    inner class VarToken(var name: String, val params: Parameters = Parameters.Empty) : DynamicVar {
        fun get(): Any? {
            return try {
                resolveVarUnwrapped(name, params)
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Fail resolve $name: ${e.message}")
            }
        }

        fun getForString(alreadyGot: Any? = Unit): String {
            return try {
                val obj = (if (alreadyGot == Unit) get() else alreadyGot) ?: "{ERR: not found $name}"
                return resolveVarForString(obj, params) ?: "{ERR($name): resolve null}"
            } catch (e: IllegalArgumentException) {
                "{ERR: ${e.message}}"
            }
        }

        override fun VarString.resolve(params: Parameters): Any? = get()
        override fun toString(): String {
            return "VarToken(name='$name', params=$params)"
        }
    }

    /**
     * will add [vars] to child as fallback
     */
    fun createChild(newText: String = text, vars: Map<String, Any?> = emptyMap()): VarString =
        VarString(newText, vars, parent = this)

    /** 解析a.b.c变量,末端未unwrap */
    fun resolveVar(keys: List<String>): Any? {
        cache?.get(keys)?.let { return it }
        loop@
        for (sp in keys.size downTo 1) {
            val key = keys.subList(0, sp)
            val keyStr = key.joinToString(".")
            var v: Any = cache?.get(key) ?: vars[keyStr] ?: if (keyStr in vars) return null else continue //overwrite null in vars in sub scoop, so we return
            if (sp < keys.size) {
                for (resolved in sp until keys.size) {
                    v = v.unwrap() ?: continue@loop
                    cache?.put(keys.subList(0, resolved), v)
                    v = resolveVarChild(v, keys[resolved]) ?: continue@loop
                }
            }
            //success
            cache?.put(keys, v)
            return v
        }
        return parent()?.resolveVar(keys)
    }

    fun resolveVarUnwrapped(keys: String, params: Parameters = Parameters.Empty): Any? {
        val key = keys.split('.')
        return resolveVar(key)?.unwrap(params = params)
    }

    fun resolveVarForString(v: Any, params: Parameters = Parameters.Empty): String? {
        val obj = v.unwrap(params = params) ?: return null
        if (obj is String) return obj
        if (obj is VarString) return (if (obj.parent != this) obj.copy(parent = this) else obj).toString()
        return resolveVarChild(obj, ToString)?.unwrap(params)?.toString()
    }

    /** 解析a->b过程,返回值未unwrap*/
    fun resolveVarChild(obj: Any, child: String): Any? {
        //对象自己也实现了VarContainer扩展
        if (obj is VarContainer)
            obj.resolve(this, child)?.let { return it }

        //通过bindTypes解析
        var cls: Class<out Any>? = obj::class.java
        while (cls != null) {
            bindTypes[cls]?.resolve(this, obj, child)?.let { return it }
            cls.interfaces.forEach { int ->
                bindTypes[int]?.resolve(this, obj, child)?.let { return it }
            }
            cls = cls.superclass
        }
        //ToString特殊实现
        if (child == ToString) return obj.toString()
        return null
    }

    private fun TokenParser.Expr.toVarToken(): VarToken {
        var params = mutableListOf<Any>()
        var v: VarToken? = null
        for (p in tokens) {
            if (v == null) {
                check(p is TokenParser.Var) { "expect Token.Var, get $p" }
                //The first token is v, And the params is mutable, add after that.
                v = VarToken(p.name, Parameters(params))
                continue
            }
            when (p) {
                is TokenParser.Var -> params.add(VarToken(p.name))
                TokenParser.PipeToken -> {
                    params = mutableListOf(v)
                    v = null
                }

                is String -> params.add(createChild(p))
                else -> error("unexpect token: $p")
            }
        }
        require(v != null) { "Empty Expression or invalid pipe" }
        return v
    }

    fun parsed(): List<Any /*[String]|[VarToken]*/> {
        return TokenParser.parse(text).map {
            if (it is String) it else (it as TokenParser.Expr).toVarToken()
        }
    }

    @Throws(IllegalArgumentException::class)
    tailrec fun Any.unwrap(params: Parameters = Parameters.Empty): Any? {
        if (this !is DynamicVar) return this
        return resolve(params)?.unwrap(params)
    }

    override fun toString(): String {
        val parsed = parsed()
        return parsed.joinToString("") {
            if (it is String) it else (it as VarToken).getForString()
        }
    }

    companion object {
        const val ToString = "toString"
        internal val globalVars = TreeMap<String, Any>()
        internal val bindTypes = mutableMapOf<Class<out Any>, TypeBinder<Any>>()
        internal val globalContext = VarString("Global_Context", globalVars, null, cache = null)
    }
}
