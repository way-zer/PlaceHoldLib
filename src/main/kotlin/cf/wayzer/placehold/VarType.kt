package cf.wayzer.placehold

/**
 * Support:
 * [NOTFOUND]: Will not parse {var}
 * [DynamicVar]: get Var dynamic, also can use getVar() to depend on other var (T only be String when registerGlobal)
 * [TypeBinder]: use for bindType(can also use as normal var) and path finder,such as {obj.c hild.obj.child:params}
 * [PlaceHoldContext]: will add vars as fallback for nested sentence, upper vars are prefer
 * Other: Can be used by other var, or will call [toString] for Value
 */
object NOTFOUND : Throwable()

/**
 * @see TemplateHandler
 */
const val TemplateHandlerKey = "_TemplateHandler"

/**
 * A Handler for template before parse
 * register use [TemplateHandlerKey]
 */
fun interface TemplateHandler {
    fun handle(ctx: PlaceHoldContext, text: String): String

    companion object {
        fun new(body: PlaceHoldContext.(text: String) -> String) = TemplateHandler { ctx, it -> ctx.body(it) }
    }
}

fun interface DynamicVar<T : Any, G : Any> {
    /**
     * @param obj bindType obj(T) or varName(List<String>, may be empty)
     * @param params may null when no params provided
     */
    fun handle(ctx: PlaceHoldContext, obj: T, params: String?): G?

    companion object {
        fun <T : Any, G : Any> obj(body: PlaceHoldContext.(obj: T) -> G?) =
            DynamicVar { ctx, obj: T, _ -> ctx.body(obj) }

        fun <G : Any> v(body: PlaceHoldContext.() -> G?) = DynamicVar { ctx, _: Any, _ -> ctx.body() }
    }
}

@Suppress("unused")
open class TypeBinder<T : Any> {
    private val handlers = mutableMapOf<String, DynamicVar<T, out Any>>()

    /**
     * Bind handler for object to string
     */
    fun registerToString(body: DynamicVar<T, String>?) {
        registerChild("toString", body)
    }

    /**
     * register child vars,can be nested
     */
    fun registerChild(key: String, body: DynamicVar<T, out Any>?) {
        if (body == null) handlers.remove(key)
        else handlers[key] = body
    }

    open fun resolve(context: PlaceHoldContext, obj: T, child: String, params: String?): Any? {
        return handlers[child]?.handle(context, obj, params)
    }
}
