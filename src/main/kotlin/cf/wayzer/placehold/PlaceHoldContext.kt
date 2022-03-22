package cf.wayzer.placehold

import cf.wayzer.placehold.util.VarTree
import java.util.*

@Suppress("MemberVisibilityCanBePrivate", "unused")
data class PlaceHoldContext(
    val text: String,
    var vars: VarTree
) {

    /**
     * will add [vars] to child as fallback
     */
    fun createChild(newText: String = text, overlay: VarTree = VarTree.Normal()): PlaceHoldContext {
        return PlaceHoldContext(newText, VarTree.Overlay(vars, overlay))
    }

    fun getVar(name: String): Any? {
        val nameS = name.split(":", limit = 2)
        return ResolveContext(
            VarTree.Overlay(globalVars, vars), keys = nameS[0],
            params = nameS.getOrNull(1), cache = true
        ).resolve()
    }

    fun getVarString(name: String): String? {
        val nameS = name.split(":", limit = 2)
        return ResolveContext(
            VarTree.Overlay(globalVars, vars), keys = "${nameS[0]}.$ToString",
            params = nameS.getOrNull(1), cache = true
        ).resolve() as String?
    }

    fun resolveVar(v: Any, keys: String = ToString, params: String? = null, obj: Any? = null): Any? {
        return ResolveContext(v, keys, params, obj).resolve()
    }

    private inner class ResolveContext(
        v: Any,
        keys: String = ToString,
        val params: String?,
        var obj: Any? = null,
        val cache: Boolean = false
    ) {
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
                vars[resolvedKey] = v
            }
            obj = asObj(v) ?: obj
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

    companion object {
        const val ToString = "toString"
        internal val globalVars = VarTree.Normal()
        internal val bindTypes = mutableMapOf<Class<out Any>, TypeBinder<Any>>()
        internal val globalContext = PlaceHoldContext("Global_Context", VarTree.Void)
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
