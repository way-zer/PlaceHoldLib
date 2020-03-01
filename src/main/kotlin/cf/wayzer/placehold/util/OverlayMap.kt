package cf.wayzer.placehold.util

/**
 * Only Simple Impl(Main on get())
 * Not remove repeat
 */
class OverlayMap<K, V>(private val lower: Map<K, V>, private val upper: Map<K, V>) : Map<K, V> {
    override val entries: Set<Map.Entry<K, V>> = lower.entries.plus(upper.entries)
    override val keys: Set<K> = lower.keys.plus(upper.keys)
    override val size: Int = lower.size + upper.size
    override val values: Collection<V> = lower.values.plus(upper.values)
    override fun containsKey(key: K) = upper.containsKey(key) || lower.containsKey(key)
    override fun containsValue(value: V) = upper.containsValue(value) || lower.containsValue(value)
    override fun get(key: K): V? = upper[key] ?: lower[key]
    override fun isEmpty(): Boolean = upper.isEmpty() && lower.isEmpty()
}
