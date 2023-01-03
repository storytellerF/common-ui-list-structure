package com.storyteller_f.ext_func_compiler

import com.google.devtools.ksp.*
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.storyteller_f.ext_func_definition.ExtFuncFlat
import com.storyteller_f.ext_func_definition.ExtFuncFlatType
import java.io.BufferedWriter
import java.io.OutputStreamWriter

class ExtFuncProcessor(private val codeGenerator: CodeGenerator) : SymbolProcessor {

    private fun generatePropertyV2(name: String) = """
val Fragment.$name get() = requireContext().$name
val View.$name get() = context.$name"""

    private fun generatePropertyV3(name: String) = """
val Fragment.$name get() = requireContext().$name
val View.$name get() = context.$name
val ViewBinding.$name get() = binding.root.context.$name"""

    class Task(val name: String, val annotation: ExtFuncFlat, val ksAnnotated: KSAnnotated)


    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(ExtFuncFlat::class.java.canonicalName)
        val ret = symbols.filter { !it.validate() }.toList()
        val filter = symbols
            .filter {
                (it is KSPropertyDeclaration || it is KSFunctionDeclaration) && it.validate()
            }
        val groupBy = filter.mapNotNull { ksAnnotated ->
            val first = ksAnnotated.getAnnotationsByType(ExtFuncFlat::class).first()
            when (ksAnnotated) {
                is KSPropertyDeclaration -> {
                    ksAnnotated.packageName.asString() to Task(ksAnnotated.simpleName.getShortName(), first, ksAnnotated)
                }
                is KSFunctionDeclaration -> {
                    ksAnnotated.packageName.asString() to Task(ksAnnotated.simpleName.getShortName(), first, ksAnnotated)
                }
                else -> null
            }
        }.groupBy { it.first }
        groupBy.forEach {
            generateForOnePackage(it.key, it.value.map { it.second })
        }
        return emptyList()
    }

    private fun generatePropertyV5(task: Task): Pair<Set<String>, String> {
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
        return imports to """
//$extra
@MainThread
inline fun <reified VM : ViewModel, ARG> Fragment.a${task.name}(
${parameterListExcludeDefault.prependIndent()}
) = ${task.name}(arg, { requireActivity().viewModelStore }, { requireActivity() }, vmProducer)
@MainThread
inline fun <reified VM : ViewModel, ARG> Fragment.p${task.name}(
${parameterListExcludeDefault.prependIndent()}
) = ${task.name}(arg, { requireParentFragment().viewModelStore }, { requireParentFragment() }, vmProducer)
"""
//        if (task.ksAnnotated is KSFunctionDeclaration) {
//            return "//${task.ksAnnotated.parameters.map { it.type }}"
//        }
//        return ""
    }

    private fun getParameterListExcludeDefault(ksAnnotated: KSFunctionDeclaration): String {
        return ksAnnotated.parameters.mapNotNull {
            if (it.hasDefault) null
            else ("${if (it.isCrossInline) "crossinline" else ""} ${it.name?.getShortName()} : ${it.type}")
        }.joinToString(",\n")
    }

    private fun generatePropertyV4(name: String, annotation: ExtFuncFlat): String {
        return """
${if (annotation.isContextReceiver) "context(Context)" else ""}
val Int.$name
get() = toFloat().dipToInt"""
    }

    private fun generateForOnePackage(packageName: String, value: List<Task>) {

        val file = codeGenerator.createNewFile(Dependencies(true, *value.mapNotNull { it.ksAnnotated.containingFile }.toSet().toTypedArray()), packageName, "ExtBuilder")
        val libraryForContext = setOf("androidx.fragment.app.Fragment", "android.view.View")
        BufferedWriter(OutputStreamWriter(file)).use { writer ->
            writer.write("package $packageName\n")

            writer.write("//${value.count()}\n")
            writer.write("//${value.map { "${it.ksAnnotated}" }.joinToString("##")}\n")

            val imports = value.map { "$packageName.${it.name}" }
            val contents = value.map {
                val name = it.name
                val type = it.annotation
                when (type.type) {
                    ExtFuncFlatType.V2 -> {
                        libraryForContext to generatePropertyV2(name)
                    }
                    ExtFuncFlatType.V3 -> {
                        libraryForContext + "androidx.viewbinding.ViewBinding" to generatePropertyV3(name)
                    }
                    ExtFuncFlatType.V4 -> {
                        setOf("android.content.Context") to generatePropertyV4(name, type)
                    }
                    ExtFuncFlatType.V5 -> {
                        generatePropertyV5(it)
                    }
                    else -> setOf<String>() to ""
                }
            }
            val list = imports + contents.map { it.first }.fold<Set<String>, Set<String>>(setOf()) { t, t1 ->
                t.plus(t1)
            }.toList()
            writer.write(list.toSet().joinToString("\n") { "import $it" })
            writer.write("\n\n")
            writer.write(contents.joinToString("\n") { it.second })
        }
    }

    private val setFold: (acc: Set<String>, Set<String>) -> Set<String> = { i, n ->
        i + n
    }

    private fun getImports(node: KSNode?): Set<String> {
        if (node == null) return emptySet<String>()
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
            is KSFunctionDeclaration -> (annotated.extensionReceiver?.let { getImports(it) }?.toSet() ?: setOf()) +
                    annotated.typeParameters.map {
                        getImports(it)
                    }.fold(setOf(), setFold) +
                    annotated.annotations.map {
                        getImports(it.annotationType)
                    }.fold(setOf(), setFold) +
                    annotated.parameters.map {
                        getImports(it)
                    }.fold(setOf(), setFold)
            is KSTypeParameter -> annotated.bounds.map { reference ->
                getImports(reference)
            }.fold(setOf(), setFold)
            is KSTypeReference -> {
                (annotated.resolve().takeIf { it.declaration.closestClassDeclaration() != null }?.declaration?.qualifiedName?.asString()?.let { setOf(it) } ?: setOf()) + getImports(annotated.element)
            }
            is KSValueParameter -> {
                getImports(annotated.type)
            }
            is KSTypeArgument -> {
                annotated.type?.let { getImports(it) } ?: setOf()
            }
            else -> setOf()
        }
    }
}

