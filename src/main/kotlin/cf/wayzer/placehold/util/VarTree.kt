package cf.wayzer.placehold.util

import cf.wayzer.placehold.PlaceHoldContext
import cf.wayzer.placehold.VarContainer

sealed class VarTree : VarContainer<Any> {
    abstract val keys: Set<String>
    abstract fun clear()
    abstract override fun resolve(ctx: PlaceHoldContext, obj: Any, child: String): Any?
    abstract operator fun set(keys: List<String>, v: Any?)
    class Normal() : VarTree() {
        private var self: Any? = null
        private var sub = emptyMap<String, VarTree>()
        override val keys: Set<String> get() = sub.keys

        constructor(map: Map<String, Any?>) : this() {
            map.forEach { (t, u) ->
                set(t.split("."), u)
            }
        }

        override fun resolve(ctx: PlaceHoldContext, obj: Any, child: String): Any? {
            return when (child) {
                "*" -> ListWithContext(obj, sub.entries.sortedBy { it.key }.map { it.value })
                Self -> self
                else -> sub[child]
            }
        }

        override operator fun set(keys: List<String>, v: Any?) {
            if (keys.contains("*")) error("Can't use '*' as key to set")
            when {
                keys.isEmpty() -> self = v
                v == null -> sub[keys[0]]?.set(keys.drop(1), null)
                else -> {
                    if (sub !is MutableMap) sub = mutableMapOf()
                    (sub as MutableMap).getOrPut(keys[0], ::Normal)[keys.drop(1)] = v
                }
            }
        }

        override fun clear() {
            (sub as? MutableMap<*, *>)?.clear()
            self = null
        }
    }

    class Overlay(private val value: VarTree, private val overlay: VarTree) : VarTree() {
        override val keys: Set<String> get() = overlay.keys + value.keys

        override fun resolve(ctx: PlaceHoldContext, obj: Any, child: String): Any? {
            if (child == "*")
                return ListWithContext(obj, keys.sorted().mapNotNull { resolve(ctx, obj, it) })
            val v = value.resolve(ctx, obj, child)
            val overV = overlay.resolve(ctx, obj, child)
            if (v is VarTree && overV is VarTree)
                return Overlay(v, overV)
            return overV ?: v
        }

        override fun set(keys: List<String>, v: Any?) {
            overlay[keys] = v
        }

        override fun clear() {
            throw UnsupportedOperationException()
        }
    }

    object Void : VarTree() {
        override val keys: Set<String> get() = emptySet()
        override fun set(keys: List<String>, v: Any?) {}
        override fun resolve(ctx: PlaceHoldContext, obj: Any, child: String): Any? = null
        override fun clear() = Unit
    }

    class ListWithContext<T>(val obj: Any, list: List<T>) : List<T> by list

    companion object {
        const val Self = "self"

        fun of(map: Map<String, Any?>): VarTree {
            return if (map.isEmpty()) Void
            else Normal(map)
        }
    }
}