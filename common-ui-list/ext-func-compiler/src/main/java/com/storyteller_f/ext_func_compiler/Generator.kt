package com.storyteller_f.ext_func_compiler

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.storyteller_f.ext_func_definition.ExtFuncFlat

internal fun generatePropertyV2(name: String) = """
    val Fragment.$name get() = requireContext().$name
    val View.$name get() = context.$name
""".trimIndent()

internal fun generatePropertyV3(name: String) = """
    val Fragment.$name get() = requireContext().$name
    val View.$name get() = context.$name
    val ViewBinding.$name get() = binding.root.context.$name
""".trimIndent()

internal fun generatePropertyV4(name: String, annotation: ExtFuncFlat): String {
    return """
    ${if (annotation.isContextReceiver) "context(Context)" else ""}
    val Int.$name
    get() = toFloat().$name
    """.trimIndent()
}

internal fun generateForV7(): String {
    return MutableList(8) {
        val i = it + 2
        val p = i - 1
        """
            data class Dao$i<${repeat("out D1", i)}>(${repeat("val d1: D1", i)})

            infix fun <${repeat("D1", i)}> Dao$p<${repeat("D1", p)}>.dao(d$i: D$i) = Dao$i(${repeat("d1", i)})
        """.trimIndent()
    }.joinToString("\n")
}

val builtinMethod = listOf("equals", "hashCode", "toString", "<init>")

internal fun generateForV8(task: ExtFuncProcessor.Task, logger: KSPLogger): String {
    val ksAnnotated = task.ksAnnotated as KSPropertyDeclaration
    val fieldName = ksAnnotated.simpleName.asString()
    val javaClass = ksAnnotated.javaClass
    logger.info(javaClass.canonicalName, ksAnnotated)
    val type = ksAnnotated.type.resolve()
    val declaration = type.declaration as KSClassDeclaration
    val methods = declaration.getAllFunctions().filter {
        !builtinMethod.contains(it.simpleName.asString())
    }.joinToString("\n") {
        val methodName = it.simpleName.asString()
        logger.info("method $methodName")
        """
            fun $methodName() = $fieldName.$methodName()
        """.trimIndent()
    }
    logger.info("type ${type.javaClass.canonicalName} declaration ${declaration.javaClass.canonicalName}")
    val parent = ksAnnotated.parent as? KSClassDeclaration ?: return ""
    logger.info("parent ${parent.javaClass}")
    val className = parent.simpleName.asString()
    return """class ${className}Impl : $className() {
        |   $methods
        |}
    """.trimMargin()
}
