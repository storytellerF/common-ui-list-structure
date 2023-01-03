package com.storyteller_f.ui_list_annotation_compiler_ksp

import com.example.ui_list_annotation_common.*
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import com.storyteller_f.annotation_defination.BindClickEvent
import com.storyteller_f.annotation_defination.BindItemHolder
import com.storyteller_f.annotation_defination.BindLongClickEvent
import java.io.BufferedWriter
import java.io.OutputStreamWriter

class Identity(val fullName: String, val name: String)

private const val className = "Temp"

class Processing(val environment: SymbolProcessorEnvironment) : SymbolProcessor {
    var count = 0
    private val zoom = UIListHolderZoom<KSAnnotated>()

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val logger = environment.logger
        count++
        if (count > 5) return emptyList()
        val viewHolders = resolver.getSymbolsWithAnnotation(BindItemHolder::class.java.canonicalName)
        val clickEvents = resolver.getSymbolsWithAnnotation(BindClickEvent::class.java.canonicalName)
        val longClickEvents = resolver.getSymbolsWithAnnotation(BindLongClickEvent::class.java.canonicalName)
        val viewHolderMap = viewHolders.groupBy {
            it.validate()
        }
        val clickEventMap = clickEvents.groupBy {
            it.validate()
        }
        val longClickEventMap = longClickEvents.groupBy {
            it.validate()
        }
        val viewHolderCount = viewHolders.count()
        val clickEventCount = clickEvents.count()
        val longClickEventCount = longClickEvents.count()

        logger.warn("count $count $viewHolderCount $clickEventCount $longClickEventCount")
        logger.warn("count $count ${viewHolderMap[true]?.count()} ${viewHolderMap[false]?.size}")
        logger.warn("count $count ${clickEventMap[true]?.count()} ${clickEventMap[false]?.size}")
        logger.warn("count $count ${longClickEventMap[true]?.count()} ${longClickEventMap[false]?.size}")
        val invalidate = viewHolderMap[false].orEmpty() + clickEventMap[false].orEmpty() + longClickEventMap[false].orEmpty()
        if (viewHolderCount == 0 && clickEventCount == 0 && longClickEventCount == 0) {
            return emptyList()
        }
        val packageName = viewHolders.map {
            (it as KSClassDeclaration).packageName.asString()
        }.min()
        zoom.addHolderEntry(processEntry(viewHolders).toList())
        zoom.addClickEvent(processEvent(clickEvents, isLong = false))
        zoom.addLongClick(processEvent(longClickEvents, isLong = true))
        val real = "$packageName.adapter_produce"
        logger.warn("package $real")
        val dependencies = Dependencies(aggregating = false, *resolver.getAllFiles().toList().toTypedArray())
        val createNewFile = environment.codeGenerator.createNewFile(dependencies, real, className)
        val importComposeLibrary = if (zoom.hasComposeView) "import androidx.compose.ui.platform.ComposeView;\n"
        else ""
        val importBindingClass = zoom.importHolders()
        val importReceiverClass = zoom.importReceiverClass()
        BufferedWriter(OutputStreamWriter(createNewFile)).use { writer ->
            writer.write("package $real")
            writer.write("//view holder count $viewHolderCount\n")
            writer.write(importComposeLibrary)
            writer.write(importBindingClass)
            writer.write(importReceiverClass)
            writer.write("class $className {\n")
            writer.write(addFunction())
            writer.write("}")
        }
        return emptyList()
    }

    private fun addFunction(): String {
        return """
            fun add() {
                
            }
        """.trimIndent()
    }

    @OptIn(KspExperimental::class)
    private fun processEvent(clickEvents: Sequence<KSAnnotated>, isLong: Boolean): Map<String, Map<String, List<Event<KSAnnotated>>>> {
        return clickEvents.doubleLayerGroupBy({
            val (itemHolderFullName, _) = getItemHolderDetail(it)
            val viewName = if (isLong) it.getAnnotationsByType(BindLongClickEvent::class).first().viewName else it.getAnnotationsByType(BindClickEvent::class).first().viewName
            itemHolderFullName to viewName
        }) {
            it as KSFunctionDeclaration
            val key = if (isLong) it.getAnnotationsByType(BindLongClickEvent::class).first().key else it.getAnnotationsByType(BindClickEvent::class).first().key
            val parent = it.parent as KSClassDeclaration
            val r = parent.identity()
            val parameterList = it.parameters.joinToString(", ") { parameter ->
                val asString = parameter.name?.asString()
                if (asString.isNullOrEmpty()) {
                    ""
                } else if (asString == "itemHolder") {
                    "viewHolder.getItemHolder()"
                } else if (asString == "binding") {
                    "inflate"
                } else {
                    "v"
                }
            }
            Event(r.name, r.fullName, it.simpleName.asString(), parameterList, key, it as KSAnnotated)
        }
    }

    fun KSDeclaration.identity() = Identity("${packageName.asString()}.${simpleName.asString()}", simpleName.asString())

    fun Identity.toPair() = fullName to name

    @OptIn(KspExperimental::class)
    private fun processEntry(viewHolders: Sequence<KSAnnotated>): Sequence<Entry<KSAnnotated>> {
        return viewHolders.map { viewHolder ->
            viewHolder as KSClassDeclaration
            val type = viewHolder.getAnnotationsByType(BindItemHolder::class).first().type
            val (itemHolderFullName, itemHolderName) = getItemHolderDetail(viewHolder)
            val (bindingName, bindingFullName) = getBindingDetail(viewHolder)
            val (viewHolderFullName, viewHolderName) = viewHolder.identity().toPair()
            Entry(itemHolderName, itemHolderFullName, mutableMapOf(type to Holder(bindingName, bindingFullName, viewHolderName, viewHolderFullName)), viewHolder as KSAnnotated)
        }
    }

    private fun getItemHolderDetail(viewHolder: KSAnnotated): Pair<String, String> {
        val ksType = viewHolder.annotations.first().arguments.first().value as KSType
        return ksType.declaration.qualifiedName.let { it!!.asString() to it.getShortName() }
    }

    private fun getBindingDetail(viewHolder: KSClassDeclaration): Pair<String, String> {
        val firstProperties = viewHolder.getAllProperties().first()
        val propertyName = firstProperties.simpleName.getShortName()
        return if (propertyName == "binding") {
            val bindingPackageName = firstProperties.packageName.asString()
            val bindingName = (firstProperties.type.element as KSClassifierReference).referencedName()
            val bindingFullName = "$bindingPackageName.databinding.$bindingName"
            Pair(bindingName, bindingFullName)
        } else {
            val asString = firstProperties.type.resolve().declaration.qualifiedName
            val bindingName = asString?.getShortName() ?: ""
            val bindingFullName = asString?.asString() ?: ""
            Pair(bindingName, bindingFullName)
        }
    }

}

class ProcessingProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return Processing(environment)
    }

}