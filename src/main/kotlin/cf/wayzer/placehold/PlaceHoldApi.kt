package cf.wayzer.placehold

import cf.wayzer.placehold.types.DateResolver
import cf.wayzer.placehold.types.ListTypeBinder
import cf.wayzer.placehold.util.VarTree
import java.util.*

@Suppress("MemberVisibilityCanBePrivate", "unused")
object PlaceHoldApi {
    var cacheMode by PlaceHoldContext.Companion::cacheMode

    /**
     * use to resolve global vars
     */
    val GlobalContext = PlaceHoldContext.globalContext
    fun getContext(text: String, vars: Map<String, Any?>) = PlaceHoldContext(text, VarTree.of(vars))
    fun replaceAll(text: String, vars: Map<String, Any?>) = getContext(text, vars).toString()
    fun String.with(vars: Map<String, Any?>) = getContext(this, vars)
    fun String.with(vararg vars: Pair<String, Any?>) = getContext(this, vars.toMap())

    /**
     * see VarType.kt for all support types
     * @param v null to remove
     */
    fun registerGlobalVar(name: String, v: Any?) {
        PlaceHoldContext.globalVars[name.split(".")] = v
    }

    /**
     * Helper function for DynamicVar
     */
    fun <T : Any> registerGlobalDynamicVar(
        name: String,
        v: PlaceHoldContext.(name: List<String>, params: String?) -> T?
    ) = registerGlobalVar(name, DynamicVar(v))

    /**
     * get binder for type
     */
    fun <T : Any> typeBinder(cls: Class<T>): TypeBinder<T> {
        @Suppress("UNCHECKED_CAST")
        return PlaceHoldContext.bindTypes.getOrPut(cls) { TypeBinder() } as TypeBinder<T>
    }

    /**
     * replace binder for type
     */
    fun <T : Any> typeBinder(cls: Class<T>, binder: TypeBinder<T>): TypeBinder<T> {
        @Suppress("UNCHECKED_CAST")
        PlaceHoldContext.bindTypes[cls] = binder as TypeBinder<Any>
        return binder
    }

    inline fun <reified T : Any> typeBinder() = typeBinder(T::class.java)

    fun resetTypeBinder(cls: Class<*>) {
        PlaceHoldContext.bindTypes.remove(cls)
    }

    fun init() {
        cacheMode = PlaceHoldContext.CacheMode.Default
        PlaceHoldContext.globalVars.clear()
        PlaceHoldContext.bindTypes.clear()

        typeBinder<Date>().registerToString(DateResolver())
        @Suppress("UNCHECKED_CAST")
        PlaceHoldContext.bindTypes[List::class.java] = ListTypeBinder() as TypeBinder<Any>
    }

    init {
        init()
    }
}
