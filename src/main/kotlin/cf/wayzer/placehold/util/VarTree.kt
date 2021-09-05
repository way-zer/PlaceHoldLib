package cf.wayzer.placehold.util

sealed class VarTree {
    abstract fun keys(prefix: List<String>): Set<String>
    abstract operator fun get(keys: List<String>): Any?
    abstract operator fun set(keys: List<String>, v: Any?)
    class Normal() : VarTree() {
        private var self: Any? = null
        private var sub = emptyMap<String, VarTree>()
        private fun setSub(key: String, v: VarTree?) {
            when {
                v == null -> (sub as? MutableMap)?.remove(key)
                sub is MutableMap -> (sub as MutableMap<String, VarTree>)[key] = v
                else -> sub = mutableMapOf(key to v)
            }
        }

        private fun getOrCreateSub(key: String): VarTree {
            var v = sub[key]
            if (v == null) {
                v = Normal()
                setSub(key, v)
            }
            return v
        }

        constructor(map: Map<String, Any?>) : this() {
            map.forEach { (t, u) ->
                set(t.split("."), u)
            }
        }

        override fun keys(prefix: List<String>): Set<String> {
            if (prefix.isEmpty()) return sub.keys
            return sub[prefix[0]]?.keys(prefix.drop(1)).orEmpty()
        }

        override operator fun set(keys: List<String>, v: Any?) {
            if (keys.contains("*")) error("Can't use '*' as key to set")
            when {
                keys.isEmpty() -> self = v
                v == null -> sub[keys[0]]?.set(keys.drop(1), v)
                else -> getOrCreateSub(keys[0])[keys.drop(1)] = v
            }
        }

        override operator fun get(keys: List<String>): Any? {
            return when {
                keys.isEmpty() -> self
                keys == listOf("*") -> sub.entries.sortedBy { it.key }.mapNotNull { it.value[emptyList()] }
                else -> sub[keys[0]]?.get(keys.drop(1))
            }
        }
    }

    class Overlay(private val value: VarTree, private val overlay: VarTree) : VarTree() {
        override fun keys(prefix: List<String>): Set<String> {
            return overlay.keys(prefix) + value.keys(prefix)
        }

        override fun get(keys: List<String>): Any? {
            if (keys.lastOrNull() == "*") {
                val prefix = keys.dropLast(1)
                return keys(prefix).sorted().mapNotNull { get(prefix + it) }
            }
            return overlay[keys] ?: value[keys]
        }

        override fun set(keys: List<String>, v: Any?) {
            overlay[keys] = v
        }
    }

    object Void : VarTree() {
        override fun keys(prefix: List<String>): Set<String> {
            return emptySet()
        }

        override fun set(keys: List<String>, v: Any?) {}
        override fun get(keys: List<String>): Any? = null
    }

    companion object {
        fun of(map: Map<String, Any?>): VarTree {
            return if (map.isEmpty()) Void
            else Normal(map)
        }
    }
}