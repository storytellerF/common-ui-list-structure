package com.storyteller_f.ext_func_compiler

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.containingFile
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.validate
import com.storyteller_f.ext_func_definition.ExtFuncFlat
import com.storyteller_f.ext_func_definition.ExtFuncFlatType
import java.io.BufferedWriter
import java.io.OutputStreamWriter

class ExtFuncProcessor(private val codeGenerator: CodeGenerator, private val logger: KSPLogger) :
    SymbolProcessor {

    class Task(val name: String, val annotation: ExtFuncFlat, val ksAnnotated: KSAnnotated)

    private var round = 0

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        round++
        val symbols = resolver.getSymbolsWithAnnotation(ExtFuncFlat::class.java.canonicalName)
        val invalidates = symbols.filter { !it.validate() }.toList()
        logger.info(
            "round $round invalidates ${invalidates.size} ${
                invalidates.joinToString {
                    it.toString()
                }
            }"
        )
        val filtered = symbols.filter {
            (it is KSPropertyDeclaration || it is KSFunctionDeclaration || it is KSClassDeclaration) && it.validate()
        }
        val groupByPackageName = filtered.mapNotNull { ksAnnotated ->
            val extFuncFlat = ksAnnotated.getAnnotationsByType(ExtFuncFlat::class).first()
            when (ksAnnotated) {
                is KSPropertyDeclaration -> taskPair(ksAnnotated, extFuncFlat)

                is KSFunctionDeclaration -> taskPair(ksAnnotated, extFuncFlat)

                is KSClassDeclaration -> taskPair(ksAnnotated, extFuncFlat)

                else -> null
            }
        }.groupBy { it.first }
        groupByPackageName.forEach { (packageName, list) ->
            generateForOnePackage(packageName, list.map { it.second }, symbols)
        }
        return invalidates
    }

    private fun taskPair(
        ksAnnotated: KSDeclaration,
        extFuncFlat: ExtFuncFlat
    ) = ksAnnotated.packageName.asString() to Task(
        ksAnnotated.simpleName.getShortName(), extFuncFlat, ksAnnotated
    )

    private fun generateForOnePackage(
        packageName: String,
        taskList: List<Task>,
        symbols: Sequence<KSAnnotated>
    ) {
        val file = codeGenerator.createNewFile(
            Dependencies(
                true,
                *taskList.mapNotNull {
                    it.ksAnnotated.containingFile
                }.toSet().toTypedArray()
            ),
            packageName,
            "ExtBuilder"
        )
        val libraryForContext = setOf("androidx.fragment.app.Fragment", "android.view.View")
        BufferedWriter(OutputStreamWriter(file)).use { writer ->
            writer.write("package $packageName\n")

            writer.write("//${taskList.count()}\n")
            writer.write("//${taskList.joinToString("##") { "${it.ksAnnotated}" }}\n")
            writer.write("//${symbols.joinToString("@@") { it.javaClass.canonicalName }}")
            val beAnnotated = taskList.map { "$packageName.${it.name}" }
            val contents = taskList.map {
                val name = it.name
                val extFuncFlat = it.annotation
                logger.info("$name ${extFuncFlat.type}")
                when (extFuncFlat.type) {
                    ExtFuncFlatType.V2 -> libraryForContext to generatePropertyV2(name)

                    ExtFuncFlatType.V3 -> libraryForContext + "androidx.viewbinding.ViewBinding" to generatePropertyV3(
                        name
                    )

                    ExtFuncFlatType.V4 -> setOf("android.content.Context") to generatePropertyV4(
                        name, extFuncFlat
                    )

                    ExtFuncFlatType.V5 -> generatePropertyV5(it)

                    ExtFuncFlatType.V6 -> setOf(
                        "androidx.lifecycle.LiveData", "androidx.lifecycle.MediatorLiveData"
                    ) to generateForV6()

                    ExtFuncFlatType.V7 -> setOf<String>() to generateForV7()
                    ExtFuncFlatType.V8 -> setOf<String>() to generateForV8(it, logger)

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
