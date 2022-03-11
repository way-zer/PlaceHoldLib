package cf.wayzer.placehold

import cf.wayzer.placehold.util.VarTree
import java.util.*

@Suppress("MemberVisibilityCanBePrivate", "unused")
data class PlaceHoldContext(
    val text: String,
    var vars: VarTree
) {
    class ResolveContext(
        v: Any,
        val keys: List<String>,
        val params: String?,
        var obj: Any? = null,
    ) {
        var v: Any = v
            set(value) {
                obj = asObj(value) ?: obj
                field = value
            }
        var resolved = 0
        val resolvedKey get() = keys.subList(0, resolved)
    }

    /**
     * will add [vars] to child as fallback
     */
    fun createChild(newText: String = text, overlay: VarTree = VarTree.Normal()): PlaceHoldContext {
        return PlaceHoldContext(newText, VarTree.Overlay(vars, overlay))
    }

    /**
     * getVarValue by Name
     * @param name There are three different format for match
     * By priority: {fullMatch} -> {fullMatch:params} -> {obj.child.child:params}
     */
    fun getVar(name: String, forString: Boolean = false): Any? {
        val nameS = name.split(":", limit = 2)
        var keys = nameS[0].split(".")
        if (forString) keys = keys + ToString
        val params = nameS.getOrNull(1)

        return resolveVar(VarTree.Overlay(vars, globalVars), keys, params)
    }


    fun resolveVar(v: Any, keys: List<String> = listOf(ToString), params: String? = null, obj: Any? = null): Any? {
        return try {
            ResolveContext(v, keys, params, obj).apply { resolve() }.v
        } catch (e: NOTFOUND) {
            null
        }
    }

    private tailrec fun ResolveContext.resolve() {
        if (resolved > 0 && !resolvedKey.contains("*")) {//cache
            vars[resolvedKey] = v
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
                while (resolved < keys.size) {
                    val sub = containers.last.resolve(this@PlaceHoldContext, obj ?: keys.subList(0, resolved + 1), keys[resolved])
                    if (sub == null) break
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
                obj = asObj(newObj) ?: obj
                return resolve()
            }
            is DynamicVar<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                v = (v as DynamicVar<Any, *>).handle(this@PlaceHoldContext, obj ?: resolvedKey, params) ?: throw NOTFOUND
                return resolve()
            }
            else -> {
                if (resolved < keys.size) {
                    val newObj = typeResolve(v, keys[resolved]) ?: throw NOTFOUND
                    resolved++
                    obj = asObj(newObj) ?: v
                    v = newObj
                    return resolve()
                }
                return
            }
        }
    }

    private fun <T : Any> typeResolve(obj: T, child: String = ToString): Any? {
        var cls: Class<out Any>? = obj::class.java
        while (cls != null) {
            bindTypes[cls]?.resolve(this, obj, child)?.let { return it }
            cls.interfaces.forEach { int ->
                bindTypes[int]?.resolve(this, obj, child)?.let { return it }
            }
            cls = cls.superclass
        }
        if (child == ToString) return obj.toString()
        return null
    }

    override fun toString(): String {
        val template = getVar(TemplateHandlerKey)
            .let { (it as? TemplateHandler)?.handle(this, text) ?: text }
        return varFormat.replace(template) {
            getVar(it.groupValues[1], true)?.toString() ?: it.value
        }
    }

    companion object {
        const val Self = "self"
        const val ToString = "toString"
        internal val globalVars = VarTree.Normal()
        internal val bindTypes = mutableMapOf<Class<out Any>, TypeBinder<Any>>()
        internal val globalContext = PlaceHoldContext("Global_Context", VarTree.Void)
        private val varFormat = Regex("[{]([^{}]+)[}]")
        fun asObj(v: Any): Any? = v.takeUnless { it == NOTFOUND || it is VarContainer<*> || it is DynamicVar<*, *> }
    }
}
