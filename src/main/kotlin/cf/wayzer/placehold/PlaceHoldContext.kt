package cf.wayzer.placehold

import cf.wayzer.placehold.util.VarTree

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

    /**
     * getVarValue by Name
     * @param name There are three different format for match
     * By priority: {fullMatch} -> {fullMatch:params} -> {obj.child.child:params}
     */
    fun getVar(name: String, forString: Boolean = false): Any? {
        val nameS = name.split(":", limit = 2)
        val keys = nameS[0].split(".")
        val params = nameS.getOrNull(1)

        try {
            val mergedVars = VarTree.Overlay(vars, globalVars)
            for (i in keys.size downTo 1) {
                val subKeys = keys.subList(0, i)
                var v = mergedVars[subKeys] ?: continue
                v = resolveVar(subKeys, v, params)
                for (ii in (i + 1)..keys.size) {
                    v = typeResolve(v, keys[ii - 1], params) ?: NOTFOUND
                    vars[keys.subList(0, ii)] = v
                    v = resolveVar(subKeys, v, params)
                }
                return if (forString) typeResolve(v, params = params) ?: v.toString()
                else v
            }
            return null
        } catch (e: NOTFOUND) {
            return null
        }
    }

    fun resolveVar(keys: List<String>, v: Any, params: String?): Any {
        var res: Any = v
        when (v) {
            is NOTFOUND -> throw NOTFOUND
            is DynamicVar<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                val vv = (v as DynamicVar<List<String>, *>).handle(this, keys, params)
                if (vv != null) {
                    res = vv
                    resolveVar(keys, vv, params)
                } else res = NOTFOUND
            }
        }
        if (res != v && keys.isNotEmpty()) vars[keys] = res
        return res
    }

    fun <T : Any> typeResolve(obj: T, child: String = "toString", params: String? = null): Any? {
        var cls: Class<out Any>? = obj::class.java
        while (cls != null) {
            bindTypes[cls]?.resolve(this, obj, child, params)?.let { return it }
            cls.interfaces.forEach { int ->
                bindTypes[int]?.resolve(this, obj, child, params)?.let { return it }
            }
            cls = cls.superclass
        }
        return null
    }

    override fun toString(): String {
        val template = getVar(TemplateHandlerKey).let { it as TemplateHandler }.handle(this, text)
        return varFormat.replace(template) {
            getVar(it.groupValues[1], true)?.toString() ?: it.value
        }
    }

    companion object {
        val globalVars = VarTree.Normal()
        val bindTypes = mutableMapOf<Class<out Any>, TypeBinder<Any>>()
        internal val globalContext = PlaceHoldContext("Global_Context", VarTree.Void)
        private val varFormat = Regex("[{]([^{}]+)[}]")
    }
}
