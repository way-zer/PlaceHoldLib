package cf.wayzer.placehold

import cf.wayzer.placehold.util.OverlayMap
import cf.wayzer.placehold.util.StringList

@Suppress("MemberVisibilityCanBePrivate", "unused")
data class PlaceHoldContext(
        val text: String,
        var vars: Map<String, Any>
) {
    /**
     * will add [vars] to child as fallback
     */
    fun createChild(newText: String = text, overlay: Map<String, Any> = emptyMap()): PlaceHoldContext {
        return PlaceHoldContext(newText, OverlayMap(vars, overlay))
    }

    /**
     * getVarValue by Name
     * @param name There are three different format for match
     * By priority: {fullMatch} -> {fullMatch:params} -> {obj.child.child:params}
     */
    fun getVar(name: String, forString: Boolean = false): Any? {
        // globalVars always as the lowest fallback
        val newVars = OverlayMap(globalVars, vars)
        var obj: Any? = null//Default is Not Found
        var params: String? = null

        fun round(v: Any?, h: () -> Any?): Any? {
            return v ?: (h()?.let {
                if (forString)
                    typeResolve(it, params = params) ?: it
                else it
            })
        }


        obj = round(obj) {
            //Full match first {fullMatch}
            newVars[name]?.let { resolveVar(name, it, null) }
        }

        val sp = name.split(":", limit = 2)
        params = sp.getOrNull(1)
        obj = round(obj) {
            //Full match with params {fullMatch:params}
            return@round if (sp.size > 1 && newVars.containsKey(sp[0])) {
                resolveVar(sp[0], newVars[sp[0]], params)
            } else null
        }

        val paths = sp[0].split('.')
        obj = round(obj) {
            //use path find {obj.child.child:params}
            var v: Any? = resolveVar(paths[0], newVars[paths[0]], params)
            var i = 1
            while (v != null && i < paths.size) {
                v = resolveVar(paths[i], typeResolve(v, paths[i], params), params)
                i++
            }
            return@round if (i == paths.size) v else null
        }

        return obj
    }

    /**
     * Resolve one variable to final value
     */
    private fun resolveVar(name: String, v: Any?, params: String?): Any? {
        return when (v) {
            NOTFOUND -> null
            is PlaceHoldContext -> {
                createChild(v.text, v.vars).toString()
            }
            is DynamicVar<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                resolveVar(name, (v as DynamicVar<String, *>).run { this@PlaceHoldContext.handle(name, params) }, params)
            }
            is List<*> -> {
                if (v is StringList<*>) v
                else StringList(v.map { resolveVar(name, it, params) })
            }
            else -> v
        }
    }

    fun <T : Any> typeResolve(obj: T, child: String = "toString", params: String? = null): Any? {
        return bindTypes[obj::class.java]?.let {
            @Suppress("UNCHECKED_CAST")
            (it as TypeBinder<T>).resolve(this, obj, child, params)
        }
    }

    override fun toString(): String {
        val template = getVar(TemplateHandlerKey).let { it as TemplateHandler }.run { handle(text) }
        return varFormat.replace(template) {
            getVar(it.groupValues[1], true)?.toString() ?: it.value
        }
    }

    companion object {
        val globalVars = mutableMapOf<String, Any>()
        val bindTypes = mutableMapOf<Class<out Any>, TypeBinder<Any>>()
        internal val globalContext = PlaceHoldContext("Global_Context", emptyMap())
        private val varFormat = Regex("[{]([^{}]+)[}]")
    }
}
