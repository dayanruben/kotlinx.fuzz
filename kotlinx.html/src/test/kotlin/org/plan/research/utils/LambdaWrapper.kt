package org.plan.research.utils

import kotlin.reflect.*

class LambdaWrapper(val block: Any.() -> Unit) : KFunction<Unit> {
    operator fun invoke(any: Any) = any.block()


    override val annotations: List<Annotation>
        get() = emptyList()
    override val isAbstract: Boolean
        get() = false
    override val isExternal: Boolean
        get() = TODO("Not yet implemented")
    override val isFinal: Boolean
        get() = TODO("Not yet implemented")
    override val isInfix: Boolean
        get() = TODO("Not yet implemented")
    override val isInline: Boolean
        get() = TODO("Not yet implemented")
    override val isOpen: Boolean
        get() = TODO("Not yet implemented")
    override val isOperator: Boolean
        get() = TODO("Not yet implemented")
    override val isSuspend: Boolean
        get() = TODO("Not yet implemented")
    override val name: String
        get() = "auto generated"
    override val parameters: List<KParameter>
        get() = TODO("Not yet implemented")
    override val returnType: KType
        get() = TODO("Not yet implemented")
    override val typeParameters: List<KTypeParameter>
        get() = TODO("Not yet implemented")
    override val visibility: KVisibility?
        get() = TODO("Not yet implemented")


    override fun call(vararg args: Any?) {
        if (args.size != 1) error("Expected one argument, got ${args.size}")
        invoke(args.first()!!)
    }

    override fun callBy(args: Map<KParameter, Any?>) {
        TODO("Not yet implemented")
    }
}