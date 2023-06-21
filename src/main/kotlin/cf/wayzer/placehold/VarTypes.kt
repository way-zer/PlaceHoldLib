package cf.wayzer.placehold

/**
 * Support:
 * null: As NotFound
 * [DynamicVar]: get Var dynamic, also can use getVar() to depend on other var (T only be String when registerGlobal)
 * [VarContainer]: a var as container, can [resolve] child var.
 * [TypeBinder]: one impl of [VarContainer], to bind vars to a Type
 * [VarString]: will add vars as fallback for nested sentence, overlay vars are preferred
 * Other: Can be used by other var, or will call [toString] for Value
 */
sealed interface VarType

/**
 * @see TemplateHandler
 */
const val TemplateHandlerKey = "_TemplateHandler"

/**
 * A Handler for template before parse
 * register use [TemplateHandlerKey]
 */
fun interface TemplateHandler {
    fun handle(ctx: VarString, text: String): String

    companion object {
        fun new(body: VarString.(text: String) -> String) = TemplateHandler { ctx, it -> ctx.body(it) }
    }
}

fun interface DynamicVar<T : Any, G : Any> : VarType {
    /**
     * @param obj bindType obj(T) or varName(List<String>, may be empty)
     * @param params may null when no params provided
     */
    @Throws(IllegalArgumentException::class)
    fun handle(ctx: VarString, obj: T, params: VarString.Parameters): G?

    companion object {
        inline fun <T : Any, G : Any> obj(crossinline body: VarString.(obj: T) -> G?) =
            DynamicVar { ctx, obj: T, _ -> ctx.body(obj) }

        inline fun <G : Any> params(crossinline body: VarString.(params: VarString.Parameters) -> G?) =
            DynamicVar { ctx, _: Any, params -> ctx.body(params) }

        inline fun <G : Any> v(crossinline body: VarString.() -> G?) = DynamicVar { ctx, _: Any, _ -> ctx.body() }
    }
}

interface VarContainer<T : Any> : VarType {
    fun resolve(ctx: VarString, obj: T, child: String): Any?
}

@Suppress("unused", "MemberVisibilityCanBePrivate")
open class TypeBinder<T : Any> : VarContainer<T> {
    private val tree = mutableMapOf<String, Any>()

    /**
     * Bind handler for object to string
     */
    fun registerToString(body: DynamicVar<T, String>?) {
        registerChildAny("toString", body)
    }

    /**
     * register child vars,can be nested
     */
    fun registerChild(key: String, body: DynamicVar<T, out Any>) {
        registerChildAny(key, body)
    }

    /**
     * register child vars,can be nested
     */
    fun registerChildAny(key: String, body: Any?) {
        if (body == null)
            tree.remove(key)
        else
            tree[key] = body
    }

    override fun resolve(ctx: VarString, obj: T, child: String): Any? {
        return tree[child]
    }
}
