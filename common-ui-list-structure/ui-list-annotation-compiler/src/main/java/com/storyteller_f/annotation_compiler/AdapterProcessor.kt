package com.storyteller_f.annotation_compiler

import com.example.ui_list_annotation_common.*
import com.storyteller_f.annotation_defination.*
import com.storyteller_f.slim_ktx.indent1
import com.storyteller_f.slim_ktx.insertCode
import com.storyteller_f.slim_ktx.no
import com.storyteller_f.slim_ktx.trimInsertCode
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.PackageElement
import javax.lang.model.element.TypeElement

class AdapterProcessor : AbstractProcessor() {

    companion object {
        const val className = "HolderBuilder"
    }

    private var count = 0
    private val zoom = UIListHolderZoom<Element>()

    @Suppress("NewApi")
    override fun process(
        set: MutableSet<out TypeElement>?, roundEnvironment: RoundEnvironment?
    ): Boolean {
        count++
        println(
            "binding event map  ${zoom.debugState()} error:${roundEnvironment?.errorRaised()} over: ${roundEnvironment?.processingOver()} count $count"
        )

        processAnnotation(set, roundEnvironment)

        writeFile(roundEnvironment)
        return true
    }

    private fun writeFile(roundEnvironment: RoundEnvironment?) {
        roundEnvironment?.let { environment ->
            if (environment.processingOver()) {
                println("binding event map process: ${zoom.debugState()}")
                zoom.setTemp.forEach { (_, packageElement) ->
                    val content = createClassFileContent(
                        packageElement, zoom.holderEntryTemp, zoom
                    )
                    val sources = zoom.getAllSource()
                    val classFile = processingEnv.filer.createSourceFile(
                        "${packageElement}.adapter_produce.$className", *sources.toTypedArray()
                    )
                    classFile.openWriter().use {
                        it.write(content)
                        it.flush()
                    }
                }
            }
        }
    }

    private fun processAnnotation(set: MutableSet<out TypeElement>?, roundEnvironment: RoundEnvironment?) {
        set?.forEach { typeElement ->
            val name = typeElement.simpleName.toString()
            println(name)
            when (name) {
                "BindItemHolder" -> {
                    getHolder(roundEnvironment, typeElement)?.let { list ->
                        zoom.addHolderEntry(list)
                    }
                    processPackageInfo(roundEnvironment, typeElement)
                }
                "BindClickEvent" -> {
                    getEvent(
                        roundEnvironment, BindClickEvent::class.java
                    )?.let { map -> zoom.addClickEvent(map) }
                }
                "BindLongClickEvent" -> {
                    getEvent(
                        roundEnvironment, BindLongClickEvent::class.java
                    )?.let { map -> zoom.addLongClick(map) }
                }
            }
        }
    }

    private fun processPackageInfo(roundEnvironment: RoundEnvironment?, typeElement: TypeElement) {
        roundEnvironment?.getElementsAnnotatedWithAny(
            typeElement
        )?.let { element ->
            element.map { processingEnv.elementUtils.getPackageOf(it) }.minByOrNull {
                it.toString()
            }?.let {
                typeElement to it
            }
        }?.let { (first, second) -> zoom.logPackageInfo(first, second) }
    }

    private fun createMultiViewHolder(entry: Entry<Element>, eventMapClick: Map<String, List<Event<Element>>>, eventMapLongClick: Map<String, List<Event<Element>>>): String {
        val viewHolderBuilderContent = entry.viewHolders.map {
            val viewHolderContent = if (it.value.bindingName.endsWith("Binding")) buildViewHolder(it.value, eventMapClick, eventMapLongClick)
            else buildComposeViewHolder(it.value, eventMapClick, eventMapLongClick)
            """
                if (type.equals("${it.key}")) {
                    $1
                }//type if end
            """.trimIndent().insertCode(viewHolderContent.indent1())
        }.joinToString("\n")
        return """
            public static AbstractViewHolder<?> buildFor${entry.itemHolderName}(ViewGroup view, String type) {
                $1
                return null;
            }
            """.trimIndent().insertCode(viewHolderBuilderContent.indent1())
    }

    private fun createClassFileContent(
        packageOf: PackageElement, holderEntry: List<Entry<Element>>, zoom: UIListHolderZoom<Element>
    ): String {
        val importHolders = zoom.importHolders()
        val importReceiverClass = zoom.importReceiverClass()
        val buildAddFunction = buildAddFunction(holderEntry)
        val hasComposeView = zoom.hasComposeView
        val buildViewHolder = holderEntry.joinToString("\n") {
            val eventMapClick = zoom.clickEventMapTemp[it.itemHolderFullName] ?: mapOf()
            val eventMapLongClick = zoom.longClickEventMapTemp[it.itemHolderFullName] ?: mapOf()
            createMultiViewHolder(it, eventMapClick, eventMapLongClick)
        }

        val importComposeLibrary = if (hasComposeView) "import androidx.compose.ui.platform.ComposeView;\n"
        else ""
        val importComposeRelatedLibrary = if (hasComposeView) """
            import com.storyteller_f.view_holder_compose.EDComposeView;
            import kotlin.Unit;
            import kotlin.jvm.functions.Function1;
            """.trimIndent()
        else ""
        return """
            package $packageOf.adapter_produce;

            import static com.storyteller_f.ui_list.core.AdapterKt.getList;
            import static com.storyteller_f.ui_list.core.AdapterKt.getRegisterCenter;
            import android.content.Context;
            import com.storyteller_f.ui_list.core.AbstractViewHolder;
            import android.view.LayoutInflater;
            import android.view.ViewGroup;
            
            $1
            $2
            $3
            import com.storyteller_f.ui_list.event.ViewJava;
            $4
            /**
             * auto generated
             * @author storyteller_f
             */
            public class $className {
                $5
                $6
            }
            """.trimInsertCode(importComposeLibrary.no(), importHolders.no(), importReceiverClass.no(), importComposeRelatedLibrary.no(), buildViewHolder.indent1(), buildAddFunction.indent1())
    }

    private fun getEvent(
        roundEnvironment: RoundEnvironment?, clazz: Class<out Annotation>
    ): Map<String, Map<String, List<Event<Element>>>>? {
        val eventAnnotations = roundEnvironment?.getElementsAnnotatedWith(clazz)
        val eventMap = eventAnnotations?.doubleLayerGroupBy({ element ->
            val viewName = if (clazz.simpleName == "BindClickEvent") element.getAnnotation(BindClickEvent::class.java).viewName
            else element.getAnnotation(BindLongClickEvent::class.java).viewName
            getAnnotationFirstArgument(element)?.let {
                it.first to viewName
            }
        }) { element ->
            val parameterList = parameterList(element)
            val key = if (clazz.simpleName == "BindClickEvent") element.getAnnotation(BindClickEvent::class.java).key
            else element.getAnnotation(BindLongClickEvent::class.java).key
            Event(
                element.enclosingElement.simpleName.toString(), element.enclosingElement.toString(), element.simpleName.toString(), parameterList, key, element
            )
        }
        return eventMap
    }

    private fun parameterList(element: Element): String {
        val parameterList = element.asType().toString().let { s ->
            val start = s.indexOf("(") + 1
            val end = s.lastIndexOf(")")
            s.substring(start, end).split(",").filter { it.isNotBlank() && it.isNotEmpty() }
        }.joinToString(", ") {
            when {
                it.isEmpty() -> ""
                it.contains("android.view.View") -> {
                    "v"
                }
                it.contains("Holder") && !it.contains("Binding") -> {
                    "viewHolder.getItemHolder()"
                }
                it.contains("Binding") -> {
                    "inflate"
                }
                else -> {
                    throw UnknownError(it)
                }
            }
        }
        return parameterList
    }

    private fun getAnnotationFirstArgument(element: Element): Pair<String, String>? {
        val let = element.annotationMirrors.first()?.elementValues?.map {
            it.value
        }?.firstOrNull()?.value?.let { list ->
            val it = list.toString()
            it to it.substring(it.lastIndexOf(".") + 1)
        }
        return let
    }

    /**
     * 建立item holder 与view holder 的关系
     */
    private fun getHolder(
        roundEnvironment: RoundEnvironment?, typeElement: TypeElement
    ): List<Entry<Element>>? {
        val holderAnnotations = roundEnvironment?.getElementsAnnotatedWithAny(typeElement)
        val holderEntry = holderAnnotations?.mapNotNull { element ->
            val type = element.getAnnotation(BindItemHolder::class.java).type

            val (itemHolderNameFullName, itemHolder) = getAnnotationFirstArgument(element) ?: return@mapNotNull null
            element.enclosedElements?.map { it.asType().toString() }?.firstOrNull { it.contains("(") }?.let {
                val bindingFullName = it.substring(it.indexOf("(") + 1, it.lastIndexOf(")"))
                val bindingName = bindingFullName.substring(bindingFullName.lastIndexOf(".") + 1)
                val viewHolderName = element.simpleName.toString()
                val viewHolderFullName = element.asType().toString()
                val viewHolders = mutableMapOf(type to Holder(bindingName, bindingFullName, viewHolderName, viewHolderFullName))
                Entry(itemHolder, itemHolderNameFullName, viewHolders, element)
            }
        }
        return holderEntry
    }

    private fun buildComposeViewHolder(
        it: Holder, eventList: Map<String, List<Event<Element>>>, eventList2: Map<String, List<Event<Element>>>
    ): String {
        return """
            Context context = view.getContext();
            EDComposeView composeView = new EDComposeView(context);
            ComposeView v = composeView.getComposeView();
            ${it.viewHolderName} viewHolder = new ${it.viewHolderName}(composeView);
            composeView.setClickListener(new Function1<String, Unit>() {
                @Override
                public Unit invoke(String s) {
                    $1
                    return null;
                }
            });
            composeView.setLongClickListener(new Function1<String, Unit>() {
                @Override
                public Unit invoke(String s) {
                    $2
                    return null;
                }
            });
            return viewHolder;
            """.trimInsertCode(buildComposeClickListener(eventList).indent1(2), buildComposeClickListener(eventList2).indent1(2))
    }

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf(
            BindItemHolder::class.java.canonicalName, BindClickEvent::class.java.canonicalName, BindLongClickEvent::class.java.canonicalName
        )
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latestSupported()
    }

    private fun buildViewHolder(
        entry: Holder, eventMapClick: Map<String, List<Event<Element>>>, eventMapLongClick: Map<String, List<Event<Element>>>
    ): String {
        val buildClickListener = buildClickListener(eventMapClick, eventMapLongClick)
        return """
            Context context = view.getContext();
            ${entry.bindingName} inflate = ${entry.bindingName}.inflate(LayoutInflater.from(context), view, false);
            
            ${entry.viewHolderName} viewHolder = new ${entry.viewHolderName}(inflate);
            $1       
            return viewHolder;
            """.trimInsertCode(buildClickListener.no())
    }

    private fun buildComposeClickListener(event: Map<String, List<Event<Element>>>) = event.map {
        val clickBlock = it.value.joinToString("\n") { e ->
            val parameterList = e.parameterList
            if (e.receiver.contains("Activity")) """
                if("${e.key}".equals(viewHolder.keyed)) ViewJava.doWhenIs(context, ${e.receiver}.class, (activity) -> {
                    activity.${e.functionName}($parameterList);
                    return null;//activity return
                });//activity end
                """.trimIndent()
            else """
                if("${e.key}".equals(viewHolder.keyed)) ViewJava.findActionReceiverOrNull(composeView.getComposeView(), ${e.receiver}.class, (fragment) -> {
                    fragment.${e.functionName}($parameterList);
                    return null;//fragment return
                });//fragment end
                """.trimIndent()
        }
        """
            if (s == "${it.key}") {
                $1                
            }//if end
        """.trimInsertCode(clickBlock.indent1())
    }.joinToString("\n")

    private fun buildClickListener(event: Map<String, List<Event<Element>>>, event2: Map<String, List<Event<Element>>>): String {
        val singleClickListener = event.map {
            """
                inflate.${it.key}.setOnClickListener((v) -> {
                    $1
                });
            """.trimInsertCode(buildClickListener(it.value).indent1())
        }.joinToString("\n")
        val longClickListener = event2.map {
            """
                inflate.${it.key}.setOnLongClickListener((v) -> {
                    $1
                    return true;
                });
            """.trimInsertCode(buildClickListener(it.value).indent1())
        }.joinToString("\n")
        return singleClickListener + longClickListener
    }

    private fun buildClickListener(events: List<Event<Element>>): String {
        return events.joinToString("\n") { event ->
            val parameterList = event.parameterList
            if (event.receiver.contains("Activity")) {
                """
                    if("${event.key}" == viewHolder.keyed) ViewJava.doWhenIs(context, ${event.receiver}.class, (activity)->{
                        activity.${event.functionName}($parameterList);
                        return null;
                    });
                """.trimIndent()
            } else {
                """
                    if("${event.key}" == viewHolder.keyed) ViewJava.findActionReceiverOrNull(v, ${event.receiver}.class, (fragment) -> {
                        fragment.${event.functionName}($parameterList);
                        return null;
                    });
                """.trimIndent()
            }
        }
    }

    private fun buildAddFunction(entry: List<Entry<Element>>): String {
        var index = 0
        val addFunctions = entry.joinToString("\n") {
            """
                getRegisterCenter().put(${it.itemHolderName}.class, ${index++} + offset);
                getList().add($className::buildFor${it.itemHolderName});
            """.trimIndent()
        }
        return """
            public static int add(int offset) {
                $1
                return $index;
            }
            """.trimInsertCode(addFunctions.indent1())
    }
}
