package cf.wayzer.placehold.utils

@JvmInline
/** Union Type for String|T */
value class StringOr<T : Any>(val asT: T) {
    @Suppress("UNCHECKED_CAST")
    constructor(str: String) : this(asT = str as T)

    val isString get() = asT is String
    val asString get() = asT as String

    override fun toString(): String {
        return asT.toString()
    }
}