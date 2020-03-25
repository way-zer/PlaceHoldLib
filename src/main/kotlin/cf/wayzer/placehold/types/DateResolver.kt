package cf.wayzer.placehold.types

import cf.wayzer.placehold.DynamicVar
import cf.wayzer.placehold.PlaceHoldContext
import java.text.SimpleDateFormat
import java.util.*

class DateResolver(private val defaultFormat: String = "MM-dd") : DynamicVar<Date,String> {
    override fun PlaceHoldContext.handle(obj: Date, params: String?): String? {
        return SimpleDateFormat(params ?: defaultFormat).format(obj)
    }
}
