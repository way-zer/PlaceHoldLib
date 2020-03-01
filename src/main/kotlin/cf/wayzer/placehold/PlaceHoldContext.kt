package cf.wayzer.placehold

import cf.wayzer.placehold.util.OverlayMap
import cf.wayzer.placehold.util.StringList

@Suppress("MemberVisibilityCanBePrivate")
data class PlaceHoldContext(
        val text: String,
        var vars: Map<String, Any>
) {
    /**
     * will add [vars] to child as fallback
     */
    fun createChild(overlay: Map<String, Any>, newText: String = text): PlaceHoldContext {
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
                if (it is DynamicVar<*>) {
                    if (forString) it.getThisSting(this)
                    else resolveVar(it.resolveThis(this), params)
                } else it
            })
        }


        obj = round(obj) {
            //Full match first {fullMatch}
            newVars[name]?.let { resolveVar(it, null) }
        }

        val sp = name.split(":")
        params = sp.getOrNull(1)
        obj = round(obj) {
            //Full match with params {fullMatch:params}
            return@round if (sp.size > 1 && newVars.containsKey(sp[0])) {
                resolveVar(newVars[sp[0]], params)
            } else null
        }

        val paths = sp[0].split('.')
        obj = round(obj) {
            //use path find {obj.child.child:params}
            var v: Any? = resolveVar(newVars[paths[0]], params)
            var i = 1
            while (i < paths.size && v is DynamicVar<*>) {
                v = resolveVar(v.resolveChild(this, paths[i]), params)
                i++
            }
            return@round if (i == paths.size) v else null
        }

        return obj
    }

    /**
     * Resolve one variable to final value
     */
    private fun resolveVar(v: Any?, params: String?): Any? {
        return when (v) {
            NOTFOUND -> null
            is PlaceHoldContext -> {
                createChild(v.vars, v.text).toString()
            }
            is DynamicResolver<*> -> {
                resolveVar(v.handle(this, params), params)
            }
            is List<*> -> {
                if (v is StringList<*>) v
                else StringList(v.map { resolveVar(it, params) })
            }
            else -> {
                if (bindTypes.containsKey(v::class.java))
                    resolveVar(bindTypes[v::class.java]?.invoke(v), params)
                else v
            }
        }
    }

    override fun toString(): String {
        return varFormat.replace(text) {
            getVar(it.groupValues[1], true)?.toString() ?: it.value
        }
    }

    companion object {
        val globalVars = mutableMapOf<String, Any>()
        val bindTypes = mutableMapOf<Class<Any>, TypeBinder<Any>>()
        private val varFormat = Regex("[{]([^{}]+)[}]")
    }
}
