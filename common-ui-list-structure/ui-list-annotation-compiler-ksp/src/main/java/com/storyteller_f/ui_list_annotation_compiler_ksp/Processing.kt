package com.storyteller_f.ui_list_annotation_compiler_ksp

import com.example.ui_list_annotation_common.Holder
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.visitor.KSDefaultVisitor
import com.storyteller_f.annotation_defination.BindItemHolder
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.lang.StringBuilder

class Processing(val environment: SymbolProcessorEnvironment) : SymbolProcessor {
    var count = 1
    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        count++
        if (count > 5) return emptyList()
        val symbolsWithAnnotation = resolver.getSymbolsWithAnnotation(BindItemHolder::class.java.canonicalName)
        val string = StringBuilder()
        val map = symbolsWithAnnotation.map { ksAnnotated ->
            ksAnnotated as KSClassDeclaration
            val first = ksAnnotated.annotations.first().arguments.first()
            val value = first.value as KSType
            val (itemHolderFullName, itemHolderName) = value.declaration.qualifiedName.let { it?.asString() to it?.getShortName() }
            val type = ksAnnotated.getConstructors().first().parameters.first()
            val javaClass = type.accept(object : KSDefaultVisitor<Unit, Unit>() {
                override fun defaultHandler(node: KSNode, data: Unit) {
                    string.append("//").appendLine((node as KSValueParameter).type.resolve())
                }

                override fun visitValueParameter(valueParameter: KSValueParameter, data: Unit) {
                    super.visitValueParameter(valueParameter, data)
                    string.appendLine("//${valueParameter.type.resolve()}")
                }
            }, Unit)
            environment.logger.logging("$javaClass")
            type
        }.joinToString(",")
        val createNewFile = environment.codeGenerator.createNewFile(Dependencies(aggregating = false, *resolver.getAllFiles().toList().toTypedArray()), "com.storyteller_f.test", "Test$count")
        BufferedWriter(OutputStreamWriter(createNewFile)).use { writer ->
            writer.write("//${symbolsWithAnnotation.count()}\n")
            writer.write("//$map\n")
            writer.write("$string\n")
        }
        return emptyList()
    }

}

class ProcessingProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return Processing(environment)
    }

}