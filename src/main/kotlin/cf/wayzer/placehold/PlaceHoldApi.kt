package cf.wayzer.placehold

import cf.wayzer.placehold.types.DateResolver
import cf.wayzer.placehold.types.ListTypeBinder
import cf.wayzer.placehold.types.StdVariable
import java.util.*

@Suppress("MemberVisibilityCanBePrivate", "unused")
object PlaceHoldApi {
    /**
     * use to resolve global vars
     */
    val GlobalContext = VarString.globalContext
    fun getContext(text: String, vars: Map<String, Any?>) = VarString(text, vars)
    fun replaceAll(text: String, vars: Map<String, Any?>) = getContext(text, vars).toString()
    fun String.with(vars: Map<String, Any?>) = getContext(this, vars)
    fun String.with(vararg vars: Pair<String, Any?>) = getContext(this, vars.toMap())

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
    fun <T : Any> registerGlobalDynamicVar(
        name: String,
        v: VarString.(name: List<String>, params: VarString.Parameters) -> T?
    ) = registerGlobalVar(name, DynamicVar(v))

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

        typeBinder<Date>().registerToString(DateResolver())
        @Suppress("UNCHECKED_CAST")
        VarString.bindTypes[List::class.java] = ListTypeBinder() as TypeBinder<Any>
        StdVariable.register()
    }

    init {
        init()
    }
}
