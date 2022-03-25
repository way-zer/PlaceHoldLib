package cf.wayzer.placehold.types

import cf.wayzer.placehold.DynamicVar
import cf.wayzer.placehold.PlaceHoldContext
import cf.wayzer.placehold.TypeBinder
import cf.wayzer.placehold.util.VarTree

class ListTypeBinder : TypeBinder<List<*>>() {
    override fun resolve(ctx: PlaceHoldContext, obj: List<*>, child: String): Any? {
        return when (child) {
            "toString" -> DynamicVar.params { params ->
                val objRaw = (obj as? VarTree.ListWithContext<*>)?.obj ?: obj
                obj.mapNotNull { it?.let { ctx.resolveVar(it, obj=objRaw)?.toString() } }
                    .joinToString(params ?: ",")
            }
            "first" -> obj.firstOrNull()
            "last" -> obj.lastOrNull()
            else -> {
                val i = child.toIntOrNull() ?: return super.resolve(ctx, obj, child)
                obj.getOrNull(i)
            }
        }
    }
}