package com.storyteller_f.ext_func_compiler

import com.google.devtools.ksp.closestClassDeclaration
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSCallableReference
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueParameter


fun getParameterListExcludeDefaultList(ksAnnotated: KSFunctionDeclaration) =
    ksAnnotated.parameters.mapNotNull {
        if (it.hasDefault) null
        else ("${if (it.isCrossInline) "crossinline" else ""} ${it.name?.getShortName()} : ${it.type}")
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

fun getImports(annotated: KSAnnotated?): Set<String> {
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

fun repeat(template: String, count: Int, start: Int = 1, sp: String = ", ", end: String = ""): String {
    val s = MutableList(count) {
        template.replace("1", (it + start).toString())
    }.joinToString(sp)
    return s + if (s.isNotEmpty()) end else ""
}