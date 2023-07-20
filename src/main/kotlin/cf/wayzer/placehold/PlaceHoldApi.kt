package cf.wayzer.placehold

import cf.wayzer.placehold.types.ListTypeBinder
import cf.wayzer.placehold.types.StdVariable

@Suppress("MemberVisibilityCanBePrivate", "unused")
object PlaceHoldApi {
    /**
     * use to resolve global vars
     */
    val GlobalContext = VarString.globalContext
    fun getVarString(text: String, vars: Map<String, Any?>) = VarString(text, vars)
    fun replaceAll(text: String, vars: Map<String, Any?>) = getVarString(text, vars).toString()
    fun String.with(vars: Map<String, Any?>) = getVarString(this, vars)
    fun String.with(vararg vars: Pair<String, Any?>) = getVarString(this, vars.toMap())

    /**
     * see VarType.kt for all support types
     * @param v null to remove
     */
    fun registerGlobalVar(name: String, v: Any?) {
        if (v == null)
            VarString.globalVars.remove(name)
        else
            VarString.globalVars[name] = v
    }

    /**
     * Helper function for DynamicVar
     */
    fun registerGlobalDynamicVar(name: String, v: DynamicVar) = registerGlobalVar(name, v)

    /**
     * get binder for type
     */
    fun <T : Any> typeBinder(cls: Class<T>): TypeBinder<T> {
        @Suppress("UNCHECKED_CAST")
        return VarString.bindTypes.getOrPut(cls) { TypeBinder() } as TypeBinder<T>
    }

    /**
     * replace binder for type
     */
    fun <T : Any> typeBinder(cls: Class<T>, binder: TypeBinder<T>): TypeBinder<T> {
        @Suppress("UNCHECKED_CAST")
        VarString.bindTypes[cls] = binder as TypeBinder<Any>
        return binder
    }

    inline fun <reified T : Any> typeBinder() = typeBinder(T::class.java)

    fun resetTypeBinder(cls: Class<*>) {
        VarString.bindTypes.remove(cls)
    }

    fun init() {
        VarString.globalVars.clear()
        VarString.bindTypes.clear()

        @Suppress("UNCHECKED_CAST")
        VarString.bindTypes[List::class.java] = ListTypeBinder() as TypeBinder<Any>
        StdVariable.register()
    }

    init {
        init()
    }
}
