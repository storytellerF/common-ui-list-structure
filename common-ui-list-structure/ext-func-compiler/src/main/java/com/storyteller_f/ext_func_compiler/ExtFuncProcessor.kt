package com.storyteller_f.ext_func_compiler

import com.google.devtools.ksp.*
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.storyteller_f.ext_func_definition.ExtFuncFlat
import com.storyteller_f.ext_func_definition.ExtFuncFlatType
import java.io.BufferedWriter
import java.io.OutputStreamWriter

class ExtFuncProcessor(private val codeGenerator: CodeGenerator, private val logger: KSPLogger) : SymbolProcessor {

    class Task(val name: String, val annotation: ExtFuncFlat, val ksAnnotated: KSAnnotated)

    private var round = 0

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        logger.warn("$round")
        round++
        val symbols = resolver.getSymbolsWithAnnotation(ExtFuncFlat::class.java.canonicalName)
        val notValidates = symbols.filter { !it.validate() }.toList()
        logger.warn("round $round not validates ${notValidates.joinToString { 
            it.toString()
        }}")
        val filter = symbols
            .filter {
                (it is KSPropertyDeclaration || it is KSFunctionDeclaration || it is KSClassDeclaration) && it.validate()
            }
        val groupByPackageName = filter.mapNotNull { ksAnnotated ->
            val extFuncFlat = ksAnnotated.getAnnotationsByType(ExtFuncFlat::class).first()
            when (ksAnnotated) {
                is KSPropertyDeclaration -> {
                    ksAnnotated.packageName.asString() to Task(ksAnnotated.simpleName.getShortName(), extFuncFlat, ksAnnotated)
                }

                is KSFunctionDeclaration -> {
                    ksAnnotated.packageName.asString() to Task(ksAnnotated.simpleName.getShortName(), extFuncFlat, ksAnnotated)
                }

                is KSClassDeclaration -> {
                    ksAnnotated.packageName.asString() to Task(ksAnnotated.simpleName.getShortName(), extFuncFlat, ksAnnotated)
                }

                else -> null
            }
        }.groupBy { it.first }
        groupByPackageName.forEach { (packageName, list) ->
            generateForOnePackage(packageName, list.map { it.second }, symbols)
        }
        return emptyList()
    }

    private fun generateForOnePackage(packageName: String, taskList: List<Task>, symbols: Sequence<KSAnnotated>) {

        val file = codeGenerator.createNewFile(Dependencies(true, *taskList.mapNotNull { it.ksAnnotated.containingFile }.toSet().toTypedArray()), packageName, "ExtBuilder")
        val libraryForContext = setOf("androidx.fragment.app.Fragment", "android.view.View")
        BufferedWriter(OutputStreamWriter(file)).use { writer ->
            writer.write("package $packageName\n")

            writer.write("//${taskList.count()}\n")
            writer.write("//${taskList.joinToString("##") { "${it.ksAnnotated}" }}\n")
            writer.write("//${symbols.joinToString("@@") {it.javaClass.canonicalName}}")
            val beAnnotated = taskList.map { "$packageName.${it.name}" }
            val contents = taskList.map {
                val name = it.name
                val extFuncFlat = it.annotation
                logger.warn("$name ${extFuncFlat.type}")
                when (extFuncFlat.type) {
                    ExtFuncFlatType.V2 -> libraryForContext to generatePropertyV2(name)

                    ExtFuncFlatType.V3 -> libraryForContext + "androidx.viewbinding.ViewBinding" to generatePropertyV3(name)

                    ExtFuncFlatType.V4 -> setOf("android.content.Context") to generatePropertyV4(name, extFuncFlat)

                    ExtFuncFlatType.V5 -> generatePropertyV5(it)

                    ExtFuncFlatType.V6 -> setOf("androidx.lifecycle.LiveData", "androidx.lifecycle.MediatorLiveData") to generateForV6()
                    ExtFuncFlatType.V7 -> setOf<String>() to generateForV7()

                    else -> setOf<String>() to ""
                }
            }
            val imports = beAnnotated.toSet() + contents.map { it.first }.fold(setOf()) { t, t1 ->
                t.plus(t1)
            }
            writer.write(imports.joinToString("\n") { "import $it" })
            writer.write("\n\n")
            writer.write(contents.joinToString("\n") { it.second })
        }
    }
}

