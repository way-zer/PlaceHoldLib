package cf.wayzer.placehold.types

import cf.wayzer.placehold.DynamicVar
import cf.wayzer.placehold.VarString
import java.text.SimpleDateFormat
import java.util.*

class DateResolver(private val defaultFormat: String = "MM-dd") : DynamicVar<Date, String> {
    override fun handle(ctx: VarString, obj: Date, params: VarString.Parameters): String? {
        return SimpleDateFormat(params.getOrNull<VarString>(0)?.text ?: defaultFormat).format(obj)
    }
}
