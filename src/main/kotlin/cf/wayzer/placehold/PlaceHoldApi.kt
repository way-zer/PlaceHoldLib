package cf.wayzer.placehold

@Suppress("MemberVisibilityCanBePrivate", "unused")
object PlaceHoldApi {
    fun getContext(text: String, vars: Map<String, Any>) = PlaceHoldContext(text, vars)
    fun replaceAll(text: String, vars: Map<String, Any>) = getContext(text, vars).toString()
    fun String.with(vars: Map<String, Any>) = getContext(this, vars)
    fun String.with(vararg vars: Pair<String, Any>) = getContext(this, vars.toMap())
    /**
     * see VarType.kt for all support types
     * @param v null to remove
     */
    fun registerGlobalVar(name: String, v: Any?) {
        if (v == null) PlaceHoldContext.globalVars.remove(name)
        else PlaceHoldContext.globalVars[name] = v
    }

    /**
     * register binder for type
     * @param v null to remove
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> registerTypeBinder(cls: Class<T>, v: TypeBinder<T>?) {
        if (v == null) PlaceHoldContext.bindTypes.remove(cls as Class<Any>)
        else PlaceHoldContext.bindTypes[cls as Class<Any>] = v as TypeBinder<Any>
    }
}
