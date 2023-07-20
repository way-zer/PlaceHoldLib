package cf.wayzer.placehold.types

import cf.wayzer.placehold.DynamicVar
import cf.wayzer.placehold.PlaceHoldApi
import cf.wayzer.placehold.VarString
import java.text.SimpleDateFormat
import java.util.Date
import java.util.NavigableMap

object StdVariable {
    fun register() {
        PlaceHoldApi.registerGlobalVar("join", DynamicVar { params ->
            val list = params.get<Iterable<*>>(0)
            val separator = params.get<String>(1,",")
            val template = params.getOrNull<VarString>(2)
            list.asSequence().mapNotNull {
                val v = it?.unwrap() ?: return@mapNotNull null
                template?.createChild(vars = mapOf("it" to v)) ?: resolveVarForString(v)
            }.joinToString(separator)
        })
        PlaceHoldApi.registerGlobalVar("listPrefix", DynamicVar { params ->
            val prefix = params.get<VarString.VarToken>(0).name + "."
            val allVars = sortedSetOf<String>()
            var ctx = this
            while (true) {
                val vars = ctx.vars
                allVars += if (vars is NavigableMap) vars.tailMap(prefix).keys.takeWhile { it.startsWith(prefix) }
                else vars.keys.filter { it.startsWith(prefix) }
                ctx = ctx.parent() ?: break
            }
            allVars.map { VarToken(it) }
        })
        PlaceHoldApi.registerGlobalVar("or", DynamicVar {
            val target = it.get<VarString.VarToken>(0)
            val fallback = it.get<String>(1)
            val obj = target.get() ?: return@DynamicVar fallback
            target.getForString(obj)
        })
        PlaceHoldApi.registerGlobalVar("date", DynamicVar {
            val date = it.get<Any>(0)
            val format = it.get<String>(1, "MM-dd")
            SimpleDateFormat(format).format(date)
        })
    }
}