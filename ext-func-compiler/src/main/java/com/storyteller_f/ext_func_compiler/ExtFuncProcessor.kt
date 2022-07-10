package com.storyteller_f.ext_func_compiler

import com.example.ext_func_definition.ExtFuncFlat
import com.example.ext_func_definition.ExtFuncFlatType
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.containingFile
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.validate
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

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(ExtFuncFlat::class.java.canonicalName)
        val ret = symbols.filter { !it.validate() }.toList()
        val fileName = "ExtFuncBuilder"
        val packageName = symbols.firstOrNull()?.containingFile?.packageName?.asString() ?: return emptyList()
        val filter = symbols
            .filter {
                it is KSPropertyDeclaration && it.validate()
            }
        val file = codeGenerator.createNewFile(Dependencies(true, *symbols.mapNotNull { it.containingFile }.toSet().toTypedArray()), packageName, fileName)
        val elements = listOf("androidx.fragment.app.Fragment", "android.view.View")
        BufferedWriter(OutputStreamWriter(file)).use { writer ->
            writer.write("package $packageName\n")

            writer.write("//${filter.count()} ${symbols.count()} ${ret.size} $packageName ${filter.map { f -> f.toString() }.joinToString(",")}\n")
            writer.write("//${symbols.map { it.javaClass.toString() + it.validate() }.joinToString("##")}\n")
            val (imports, contents) = filter.mapNotNull { ksAnnotated ->
                val first = ksAnnotated.getAnnotationsByType(ExtFuncFlat::class).first()
                if (ksAnnotated is KSPropertyDeclaration) {
                    ksAnnotated.packageName.asString() to (ksAnnotated.simpleName.getShortName() to first)
                } else null
            }.groupBy { it.first }.run {
                keys.mapNotNull { k -> get(k)?.map { "$k.${it.second.first}" } }.flatten() to this.flatMap {
                    it.value
                }.map { (_, ele) ->
                    val (name, type) = ele
                    (when (type.type) {
                        ExtFuncFlatType.v2 -> {
                            elements to generatePropertyV2(name)
                        }
                        ExtFuncFlatType.v3 -> {
                            elements + "androidx.viewbinding.ViewBinding" to generatePropertyV3(name)
                        }
                        ExtFuncFlatType.v4 -> {
                            listOf("android.content.Context") to generatePropertyV4(name, type)
                        }
                        else -> listOf<String>() to ""
                    })
                }
            }
            val list = imports + contents.flatMap { it.first }
//            writer.write("//imports ${imports.joinToString(",")}")
//            writer.write("\n")
//            writer.write("//list ${list.joinToString(",")}")
//            writer.write("\n")
            writer.write(list.toSet().joinToString("\n") { "import $it" })
            writer.write("\n\n")
            writer.write(contents.joinToString("\n") { it.second })
        }

        return emptyList()
    }

    private fun generatePropertyV4(name: String, annotation: ExtFuncFlat): String {
        return """
${if (annotation.isContextReceiver) "context(Context)" else ""}
val Int.$name
get() = toFloat().dipToInt"""
    }
}

