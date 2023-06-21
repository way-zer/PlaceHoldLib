package cf.wayzer.placehold.types

import cf.wayzer.placehold.VarString
import cf.wayzer.placehold.TypeBinder

class ListTypeBinder : TypeBinder<List<*>>() {
    override fun resolve(ctx: VarString, obj: List<*>, child: String): Any? {
        return when (child) {
            "toString" -> ctx.VarToken("join", VarString.Parameters(listOf(obj))).getForString()
            "first" -> obj.firstOrNull()
            "last" -> obj.lastOrNull()
            else -> {
                val i = child.toIntOrNull() ?: return super.resolve(ctx, obj, child)
                obj.getOrNull(i)
            }
        }
    }
}