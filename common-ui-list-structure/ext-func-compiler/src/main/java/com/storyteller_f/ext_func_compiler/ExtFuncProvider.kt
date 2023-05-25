package com.storyteller_f.ext_func_compiler

import com.google.devtools.ksp.closestClassDeclaration
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSCallableReference
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueParameter
import com.storyteller_f.ext_func_definition.ExtFuncFlat
import com.storyteller_f.slim_ktx.yes

class ExtFuncProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        val logger = environment.logger
        //error 以上的会导致注解退出，谨慎使用
        return ExtFuncProcessor(environment.codeGenerator, logger)
    }

}

internal fun generatePropertyV2(name: String) = """
    val Fragment.$name get() = requireContext().$name
    val View.$name get() = context.$name
""".trimIndent()

internal fun generatePropertyV3(name: String) = """
    val Fragment.$name get() = requireContext().$name
    val View.$name get() = context.$name
    val ViewBinding.$name get() = binding.root.context.$name
""".trimIndent()

internal fun generateForV6(): String {
    return MutableList(8) {
        val count = 2 + it
        val genericList = repeat("T1?", count)
        val type = "MediatorLiveData<Dao$count<$genericList>>"

        combineDao(count, type)
    }.joinToString("\n")
}

private fun combineDao(count: Int, type: String): String {
    return """
        fun<${repeat("T1", count)}> combineDao(${repeat("s1: LiveData<T1>", count)}): $type {
            val mediatorLiveData = $type()
            ${repeat("var d1 = s1.value\n", count, sp = "\n").yes(3).prependRest()}
            ${liveDataAddSource(count).yes(3).prependRest()}
            return mediatorLiveData
        }
        """.trimIndent()
}

private fun liveDataAddSource(count: Int): String {
    return MutableList(count) {
        val current = it + 1
        """
        mediatorLiveData.addSource(s${current}) {
            d${current} = it
            mediatorLiveData.value = Dao$count(${repeat("d1", current - 1, end = ", ")}it, ${repeat("d1", count - current, current + 1)})
        }
        """.trimIndent()
    }.joinToString("\n")
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

private fun repeat(template: String, count: Int, start: Int = 1, sp: String = ", ", end: String = ""): String {
    val s = MutableList(count) {
        template.replace("1", (it + start).toString())
    }.joinToString(sp)
    return s + if (s.isNotEmpty()) end else ""
}

internal fun generatePropertyV5(task: ExtFuncProcessor.Task): Pair<Set<String>, String> {
    val extra = if (task.ksAnnotated is KSFunctionDeclaration) {
        task.ksAnnotated.parameters.map {
//                (it.type.element as KSCallableReference).typeArguments.map {
//                    it.type
//                }
            it.type.element
        }.joinToString(",")
    } else null
    val imports = getImports(task.ksAnnotated) + listOf("androidx.fragment.app.Fragment")
    val parameterListExcludeDefault = getParameterListExcludeDefault(task.ksAnnotated as KSFunctionDeclaration)
    return imports to extendVm(extra, task, parameterListExcludeDefault)
//        if (task.ksAnnotated is KSFunctionDeclaration) {
//            return "//${task.ksAnnotated.parameters.map { it.type }}"
//        }
//        return ""
}

private fun extendVm(extra: String?, task: ExtFuncProcessor.Task, parameterListExcludeDefault: String): String {
    return """
        //$extra
        @MainThread
        inline fun <reified VM : ViewModel, ARG> Fragment.a${task.name}(
            ${parameterListExcludeDefault.yes(3).prependRest()}
        ) = ${task.name}(arg, { requireActivity().viewModelStore }, { requireActivity() }, vmProducer)
        @MainThread
        inline fun <reified VM : ViewModel, ARG> Fragment.p${task.name}(
            ${parameterListExcludeDefault.yes(3).prependRest()}
        ) = ${task.name}(arg, { requireParentFragment().viewModelStore }, { requireParentFragment() }, vmProducer)
        """.trimIndent()
}

private fun getParameterListExcludeDefault(ksAnnotated: KSFunctionDeclaration): String {
    return ksAnnotated.parameters.mapNotNull {
        if (it.hasDefault) null
        else ("${if (it.isCrossInline) "crossinline" else ""} ${it.name?.getShortName()} : ${it.type}")
    }.joinToString(",\n")
}

internal fun generatePropertyV4(name: String, annotation: ExtFuncFlat): String {
    return """
    ${if (annotation.isContextReceiver) "context(Context)" else ""}
    val Int.$name
    get() = toFloat().dipToInt
""".trimIndent()
}

private val setFold: (acc: Set<String>, Set<String>) -> Set<String> = { i, n ->
    i + n
}

private fun getImports(node: KSNode?): Set<String> {
    if (node == null) return emptySet()
    return when (node) {
        is KSCallableReference -> {
            getImports(node.returnType) + node.typeArguments.map {
                getImports(it)
            }.fold(setOf(), setFold)
        }

        else -> {
            emptySet()
        }
    }
}

private fun getImports(annotated: KSAnnotated?): Set<String> {
    if (annotated == null) {
        return emptySet()
    }

    return when (annotated) {
        is KSFunctionDeclaration -> {
            getImports(annotated.extensionReceiver) +
                    annotated.typeParameters.map {
                        getImports(it)
                    }.fold(setOf(), setFold) +
                    annotated.annotations.map {
                        getImports(it.annotationType)
                    }.fold(setOf(), setFold) +
                    annotated.parameters.map {
                        getImports(it)
                    }.fold(setOf(), setFold)
        }

        is KSTypeParameter -> annotated.bounds.map { reference ->
            getImports(reference)
        }.fold(setOf(), setFold)

        is KSTypeReference -> {
            val typeString = annotated.resolve().takeIf { it.declaration.closestClassDeclaration() != null }?.declaration?.qualifiedName?.asString()
            typeString?.let { setOf(it) }.orEmpty() + getImports(annotated.element)
        }

        is KSValueParameter -> {
            getImports(annotated.type)
        }

        is KSTypeArgument -> {
            getImports(annotated.type)
        }

        else -> setOf()
    }
}