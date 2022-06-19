package com.storyteller_f.ext_func_compiler

import com.example.ext_func_definition.ExtFuncFlat
import com.google.devtools.ksp.containingFile
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.validate
import java.io.BufferedWriter
import java.io.OutputStreamWriter

class ExtFuncProcessor(val codeGenerator: CodeGenerator, val logger: KSPLogger, val options: Map<String, String>) : SymbolProcessor {

    private fun generateProperty(name: String): String {
        return """
            val Fragment.$name get() = requireContext().$name
            val View.$name get() = context.$name
        """.trimIndent()
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(ExtFuncFlat::class.java.canonicalName)
//        val ret = symbols.filter { !it.validate() }.toList()
        val fileName = "ExtFuncBuilder"
        val packageName = symbols.firstOrNull()?.containingFile?.packageName?.getQualifier() ?: return emptyList()

        val filter = symbols
            .filter {
                it is KSPropertyDeclaration && it.validate()
            }
        val file = codeGenerator.createNewFile(Dependencies(true, *symbols.mapNotNull { it.containingFile }.toSet().toTypedArray()), packageName, fileName)
        val bufferedWriter = BufferedWriter(OutputStreamWriter(file))
        bufferedWriter.use { writer ->
            writer.write("package $packageName\n")
            writer.write("import androidx.fragment.app.Fragment\nimport android.view.View\nimport com.storyteller_f.common_ui.lf\n")
            writer.write("//${filter.count()} ${symbols.count()} $packageName ${filter.map { f -> f.toString() }.joinToString(",")}\n")
            filter.mapNotNull { ksAnnotated ->
                if (ksAnnotated is KSPropertyDeclaration && ksAnnotated.extensionReceiver?.toString()?.equals("Context") == true) {
                   ksAnnotated.packageName.asString() to ksAnnotated.simpleName.getShortName()
                }
                else null
            }.groupBy { it.first }.run {
                keys.mapNotNull { k -> get(k)?.map { "import $k.${it.second}" } }.flatten().run {
                    writer.write(this.joinToString("\n"))
                }
                writer.write("\n")
                this.flatMap {
                    it.value
                }.map {
                    generateProperty(it.second)
                }.run {
                    writer.write(this.joinToString("\n"))
                }
            }
        }


        return emptyList()
    }
}

