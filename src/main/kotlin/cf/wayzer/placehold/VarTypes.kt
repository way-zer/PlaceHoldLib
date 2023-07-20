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

fun interface DynamicVar : VarType {
    /**
     * @param params may null when no params provided
     */
    @Throws(IllegalArgumentException::class)
    fun VarString.resolve(params: VarString.Parameters): Any?
}

interface VarContainer : VarType {
    fun resolve(ctx: VarString, child: String): Any?
}

@Suppress("unused", "MemberVisibilityCanBePrivate")
open class TypeBinder<T : Any> {
    fun interface ObjChild<T : Any> {
        fun VarString.resolve(obj: T): Any?
    }

    private val tree = mutableMapOf<String, Any>()

    /**
     * Bind handler for object to string
     */
    fun registerToString(body: ObjChild<T>?) {
        registerChildAny("toString", body)
    }

    /**
     * register child vars,can be nested
     */
    fun registerChild(key: String, body: ObjChild<T>) {
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

    open fun resolve(ctx: VarString, obj: T, child: String): Any? {
        val value = tree[child] ?: return null
        @Suppress("UNCHECKED_CAST")
        if (value is ObjChild<*>)
            return (value as ObjChild<T>).run { ctx.resolve(obj) }
        return value
    }
}
