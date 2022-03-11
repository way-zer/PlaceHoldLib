package cf.wayzer.placehold

import cf.wayzer.placehold.NOTFOUND.toString
import cf.wayzer.placehold.util.VarTree

/**
 * Support:
 * [NOTFOUND]: Will not parse {var}
 * [DynamicVar]: get Var dynamic, also can use getVar() to depend on other var (T only be String when registerGlobal)
 * [VarContainer]: a var as container, can [resolve] child var.
 * [TypeBinder]: one impl of [VarContainer], to bind vars to a Type
 * [PlaceHoldContext]: will add vars as fallback for nested sentence, overlay vars are preferred
 * Other: Can be used by other var, or will call [toString] for Value
 */
object NOTFOUND : Throwable("NOTFOUND")

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
        inline fun <T : Any, G : Any> obj(crossinline body: PlaceHoldContext.(obj: T) -> G?) =
            DynamicVar { ctx, obj: T, _ -> ctx.body(obj) }

        inline fun <G : Any> params(crossinline body: PlaceHoldContext.(params: String?) -> G?) =
            DynamicVar { ctx, _: Any, params -> ctx.body(params) }

        inline fun <G : Any> v(crossinline body: PlaceHoldContext.() -> G?) = DynamicVar { ctx, _: Any, _ -> ctx.body() }
    }
}

interface VarContainer<T : Any> {
    fun resolve(ctx: PlaceHoldContext, obj: T, child: String): Any?
}

@Suppress("unused", "MemberVisibilityCanBePrivate")
open class TypeBinder<T : Any> : VarContainer<T> {
    private val tree = VarTree.Normal()

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
        tree[key.split('.')] = body
    }

    override fun resolve(ctx: PlaceHoldContext, obj: T, child: String): Any? {
        return tree.resolve(ctx, obj, child)
    }
}
