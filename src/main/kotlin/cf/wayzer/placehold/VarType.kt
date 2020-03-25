package cf.wayzer.placehold

/**
 * Support:
 * [NOTFOUND]: Will not parse {var}
 * [DynamicVar]: get Var dynamic, also can use getVar() to depend on other var (T only be String when registerGlobal)
 * [TypeBinder]: use for bindType(can also use as normal var) and path finder,such as {obj.c hild.obj.child:params}
 * [PlaceHoldContext]: will add vars as fallback for nested sentence, upper vars are prefer
 * Other: Can be used by other var, or will call [toString] for Value
 */
val NOTFOUND = null

/*fun*/ interface DynamicVar<T:Any,G:Any> {
    /**
     * @param obj bindType obj(T) or varName(String)
     * @param params may null when no params provided
     */
    fun PlaceHoldContext.handle(obj:T , params: String?): G?
    companion object{
        operator fun <T : Any> invoke(v: PlaceHoldContext.() -> T?) = object : DynamicVar<Any, T> {
            override fun PlaceHoldContext.handle(obj: Any, params: String?): T? = v()
        }
        operator fun <T : Any,G:Any> invoke(v: PlaceHoldContext.(obj: T, params: String?) -> G?) = object : DynamicVar<T, G> {
            override fun PlaceHoldContext.handle(obj: T, params: String?): G? = v(obj, params)
        }
    }
}

@Suppress("unused")
open class TypeBinder<T:Any>{
    private val handlers = mutableMapOf<String,DynamicVar<T,out Any>>()

    /**
     * Bind handler for object to string
     */
    fun registerToString(body: DynamicVar<T,String>){
        handlers["toString"]=body
    }
    /**
     * register child vars,can be nested
     */
    fun registerChild(key: String,body: DynamicVar<T,Any>){
        handlers[key]=body
    }
    open fun resolve(context: PlaceHoldContext,obj:T,child:String,params: String?):Any?{
        return handlers[child]?.run { context.handle(obj,params) }
    }
}
