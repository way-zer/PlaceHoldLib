package cf.wayzer.placehold.types

import cf.wayzer.placehold.DynamicVar
import cf.wayzer.placehold.PlaceHoldApi
import cf.wayzer.placehold.VarString
import java.util.NavigableMap

object StdVariable {
    fun register() {
        PlaceHoldApi.registerGlobalVar("join", DynamicVar.params { params ->
            val list = params.get<Iterable<*>>(0)
            val separator = params.getOrNull<VarString>(1)?.text ?: ","
            val template = params.getOrNull<VarString>(2)
            list.asSequence().mapNotNull {
                val v = it?.unwrap()?:return@mapNotNull null
                template?.createChild(vars = mapOf("it" to v)) ?: resolveVarForString(v)
            }.joinToString(separator)
        })
        PlaceHoldApi.registerGlobalVar("listPrefix", DynamicVar.params { params ->
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
        //TODO or
    }
}