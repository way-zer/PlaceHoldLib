package cf.wayzer.placehold

import cf.wayzer.placehold.util.VarTree
import java.util.*

@Suppress("MemberVisibilityCanBePrivate", "unused")
data class PlaceHoldContext(
    val text: String,
    val vars: VarTree
) {
    val cache = if (cacheMode == CacheMode.Strict) VarTree.Normal() else VarTree.Void
    val cachedVars = if (cacheMode == CacheMode.Strict) VarTree.Overlay(vars, cache) else vars

    /**
     * will add [vars] to child as fallback
     */
    fun createChild(newText: String = text, overlay: VarTree = VarTree.Void): PlaceHoldContext {
        val cached = if (cacheMode == CacheMode.Default) VarTree.Overlay(overlay, VarTree.Normal()) else overlay
        return PlaceHoldContext(newText, VarTree.Overlay(vars, cached))
    }

    fun getVar(name: String): Any? {
        val nameS = name.split(":", limit = 2)
        return ResolveContext(
            VarTree.Overlay(globalVars, cachedVars), keys = nameS[0],
            params = nameS.getOrNull(1), cache = true
        ).resolve()
    }

    fun getVarString(name: String): String? {
        val nameS = name.split(":", limit = 2)
        return ResolveContext(
            VarTree.Overlay(globalVars, cachedVars), keys = "${nameS[0]}.$ToString",
            params = nameS.getOrNull(1), cache = true
        ).resolve() as String?
    }

    /** purely, no [globalVars] and cache, for resolve [DynamicVar]*/
    fun resolveVar(v: Any, keys: String = ToString, params: String? = null, obj: Any? = null): Any? {
        return ResolveContext(v, keys, params, obj).resolve()
    }

    private inner class ResolveContext(
        v: Any,
        keys: String = ToString,
        val params: String?,
        var obj: Any? = asObj(v),
        private var cache: Boolean = false
    ) {
        init {
            if (cacheMode == CacheMode.Disable)
                cache = false
        }

        val keys = keys.split(".").dropWhile { it.isBlank() }
        var v: Any = v
            set(value) {
                obj = asObj(value) ?: obj
                field = value
            }
        var resolved = 0
        val resolvedKey get() = keys.subList(0, resolved)

        fun resolve(): Any? = try {
            resolve0()
            v
        } catch (e: NOTFOUND) {
            null
        }

        private tailrec fun resolve0() {
            if (cache && resolved > 0 && !resolvedKey.contains("*")) {//cache
                cachedVars[resolvedKey] = v
            }
            when (val vv = v) {
                is NOTFOUND -> throw NOTFOUND
                is PlaceHoldContext -> {
                    v = createChild(vv.text, vv.vars)
                    if (keys.getOrNull(resolved) == ToString) {
                        v = (v as PlaceHoldContext).toString()
                        resolved++
                    }
                    if (resolved < keys.size) throw NOTFOUND
                    return //end
                }

                is VarContainer<*> -> {
                    var newObj: Any? = null
                    val containers = LinkedList<VarContainer<Any>>()
                    @Suppress("UNCHECKED_CAST")
                    containers.add(vv as VarContainer<Any>)
                    //原地自旋解析，直到下一个值不是VarContainer
                    while (resolved < keys.size) {
                        val sub = containers.last.resolve(this@PlaceHoldContext, obj ?: keys.subList(0, resolved + 1), keys[resolved]) ?: break
                        resolved++
                        if (sub is VarContainer<*>)
                            @Suppress("UNCHECKED_CAST")
                            containers.add(sub as VarContainer<Any>)
                        else {
                            newObj = sub
                            break
                        }
                    }
                    //有可能走到VarTree的空子树中，故需要回溯
                    var first = true
                    while (newObj == null && containers.isNotEmpty()) {
                        newObj = containers.removeLast().resolve(this@PlaceHoldContext, obj ?: resolvedKey, VarTree.Self)
                        if (first) first = false
                        else resolved--
                    }
                    if (newObj == null) throw NOTFOUND
                    v = newObj
                    return resolve0()
                }

                is DynamicVar<*, *> -> {
                    @Suppress("UNCHECKED_CAST")
                    v = (v as DynamicVar<Any, *>).handle(this@PlaceHoldContext, obj ?: resolvedKey, params) ?: throw NOTFOUND
                    if (cache && params != null)
                        cache = false
                    return resolve0()
                }

                else -> {
                    if (resolved < keys.size) {
                        val newObj = typeResolve(this@PlaceHoldContext, v, keys[resolved]) ?: throw NOTFOUND
                        resolved++
                        obj = v
                        v = newObj
                        return resolve0()
                    }
                    return
                }
            }
        }
    }

    override fun toString(): String {
        val template = getVar(TemplateHandlerKey)
            .let { (it as? TemplateHandler)?.handle(this, text) ?: text }
        return varFormat.replace(template) {
            getVarString(it.groupValues[1]) ?: it.value
        }
    }

    enum class CacheMode {
        /** 关闭缓存机制 */
        Disable,

        /** 对ChildContext采用独立的缓存。但存在父域变量影响子域变量读的问题[Test.testCacheBug2] */
        Default,

        /** 对每个PlaceHoldContext采用独立的缓存，*/
        Strict
    }

    companion object {
        const val ToString = "toString"
        internal val globalVars = VarTree.Normal()
        internal val bindTypes = mutableMapOf<Class<out Any>, TypeBinder<Any>>()
        internal val globalContext = PlaceHoldContext("Global_Context", VarTree.Void)
        internal var cacheMode = CacheMode.Default

        private val varFormat = Regex("[{]([^{}]+)[}]")
        private fun asObj(v: Any): Any? = v.takeUnless { it == NOTFOUND || it is VarContainer<*> || it is DynamicVar<*, *> }
        private fun <T : Any> typeResolve(ctx: PlaceHoldContext, obj: T, child: String = ToString): Any? {
            var cls: Class<out Any>? = obj::class.java
            while (cls != null) {
                bindTypes[cls]?.resolve(ctx, obj, child)?.let { return it }
                cls.interfaces.forEach { int ->
                    bindTypes[int]?.resolve(ctx, obj, child)?.let { return it }
                }
                cls = cls.superclass
            }
            if (child == ToString) return obj.toString()
            return null
        }
    }
}
