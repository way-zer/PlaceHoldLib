package cf.wayzer.placehold

import cf.wayzer.placehold.types.DateResolver
import cf.wayzer.placehold.util.VarTree
import java.util.*

@Suppress("MemberVisibilityCanBePrivate", "unused")
object PlaceHoldApi {
    /**
     * use to resolve global vars
     */
    val GlobalContext = PlaceHoldContext.globalContext
    fun getContext(text: String, vars: Map<String, Any>) = PlaceHoldContext(text, VarTree.Normal(vars))
    fun replaceAll(text: String, vars: Map<String, Any>) = getContext(text, vars).toString()
    fun String.with(vars: Map<String, Any>) = getContext(this, vars)
    fun String.with(vararg vars: Pair<String, Any>) = getContext(this, vars.toMap())

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
    fun <T : Any> registerGlobalDynamicVar(name: String, v: PlaceHoldContext.(name: List<String>, params: String?) -> T?) = registerGlobalVar(name, DynamicVar(v))

    /**
     * get binder for type
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> typeBinder(cls: Class<T>): TypeBinder<T> {
        return PlaceHoldContext.bindTypes.getOrPut(cls) { TypeBinder() } as TypeBinder<T>
    }

    inline fun <reified T : Any> typeBinder() = typeBinder(T::class.java)

    init {
        registerGlobalVar(TemplateHandlerKey, TemplateHandler { _, it -> it })//Keep it origin
        typeBinder<Date>().registerToString(DateResolver())
    }
}
