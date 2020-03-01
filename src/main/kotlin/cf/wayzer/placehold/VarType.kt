package cf.wayzer.placehold

/**
 * Support:
 * [NOTFOUND]: Will not parse {var}
 * [DynamicResolver]: get Var dynamic, also can use getVar() to depend on other var
 * [DynamicVar]: use for path finder,such as {obj.child.obj.child:params}
 * [PlaceHoldContext]: will add vars as fallback for nested sentence, upper vars are prefer
 * Other: Can be used by other var, or will call [toString] for Value
 */
val NOTFOUND = null

interface DynamicResolver<T : Any> {
    /**
     * @param params may null when no params provided
     */
    fun handle(context: PlaceHoldContext, params: String?): T?

    companion object {
        @Suppress("unused")
        fun <T : Any> new(handler: PlaceHoldContext.(params: String?) -> T?) = object : DynamicResolver<T> {
            override fun handle(context: PlaceHoldContext, params: String?): T? = handler(context, params)
        }
    }
}

abstract class DynamicVar<T> {
    private val allHandler = mutableMapOf<String, PlaceHoldContext.(T?) -> Any?>()
    /**
     * Call in init{}
     * @param handler input [resolveThis]
     */
    protected fun registerChild(key: String, handler: PlaceHoldContext.(T?) -> Any?) {
        allHandler[key] = handler
    }

    open fun resolveThis(context: PlaceHoldContext): T? {
        return null
    }

    open fun getThisSting(context: PlaceHoldContext): String? {
        return "BAD for Object"
    }

    open fun resolveChild(context: PlaceHoldContext, key: String): Any? {
        return allHandler[key]?.invoke(context, resolveThis(context))
    }
}
/**
 * use [PlaceHoldApi.registerTypeBinder] to register
 */
typealias TypeBinder<T> = (obj: T) -> Any
