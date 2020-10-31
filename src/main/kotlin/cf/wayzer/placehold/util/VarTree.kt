package cf.wayzer.placehold.util

sealed class VarTree {
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

        override operator fun set(keys: List<String>, v: Any?) {
            when {
                keys.isEmpty() -> self = v
                v == null -> sub[keys[0]]?.set(keys.drop(1), v)
                else -> getOrCreateSub(keys[0])[keys.drop(1)] = v
            }
        }

        override operator fun get(keys: List<String>): Any? {
            return if (keys.isEmpty())
                self
            else {
                sub[keys[0]]?.get(keys.subList(1, keys.size))
            }
        }
    }

    class Overlay(private val value: VarTree, private val overlay: VarTree) : VarTree() {
        override fun get(keys: List<String>): Any? {
            return overlay[keys] ?: value[keys]
        }

        override fun set(keys: List<String>, v: Any?) {
            overlay[keys] = v
        }
    }

    object Void : VarTree() {
        override fun set(keys: List<String>, v: Any?) {}
        override fun get(keys: List<String>): Any? = null
    }
}