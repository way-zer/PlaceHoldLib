package cf.wayzer.placehold.types

import cf.wayzer.placehold.DynamicResolver
import cf.wayzer.placehold.PlaceHoldContext
import java.text.SimpleDateFormat
import java.util.*

class DateResolver(private val date: Date, private val defaultFormat: String = "MM-dd") : DynamicResolver<String> {
    override fun handle(context: PlaceHoldContext, params: String?): String? {
        return SimpleDateFormat(params ?: defaultFormat).format(date)
    }
}
