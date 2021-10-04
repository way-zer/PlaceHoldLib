package cf.wayzer.placehold.types

import cf.wayzer.placehold.PlaceHoldContext
import cf.wayzer.placehold.TypeBinder

class ListTypeBinder : TypeBinder<List<*>>() {
    override fun resolve(context: PlaceHoldContext, obj: List<*>, child: String, params: String?): Any? {
        return when (child) {
            "toString" -> obj.filterNotNull().joinToString(params ?: ",") {
                val v = context.resolveVar(emptyList(), it, null)
                context.typeResolve(v) as String? ?: v.toString()
            }
            "first" -> obj.firstOrNull()
            "last" -> obj.lastOrNull()
            else -> {
                val i = child.toIntOrNull() ?: return super.resolve(context, obj, child, params)
                obj.getOrNull(i)
            }
        }
    }
}