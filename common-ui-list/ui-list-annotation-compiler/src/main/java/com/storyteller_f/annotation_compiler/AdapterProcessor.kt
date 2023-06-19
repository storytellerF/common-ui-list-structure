package com.storyteller_f.annotation_compiler

import com.example.ui_list_annotation_common.*
import com.storyteller_f.annotation_defination.*
import com.storyteller_f.slim_ktx.insertCode
import com.storyteller_f.slim_ktx.no
import com.storyteller_f.slim_ktx.trimInsertCode
import com.storyteller_f.slim_ktx.yes
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
            "binding event map ${zoom.debugState()} error:${roundEnvironment?.errorRaised()} over: ${roundEnvironment?.processingOver()} count $count"
        )

        processAnnotation(set, roundEnvironment)

        writeFile(roundEnvironment)
        return true
    }

    private fun writeFile(roundEnvironment: RoundEnvironment?) {
        roundEnvironment ?: return
        if (roundEnvironment.processingOver()) {
            println("binding writeFile: ${zoom.debugState()}")
            zoom.packagesTemp.forEach { (_, packageElement) ->
                val content = createClassFileContent(
                    packageElement, zoom.holderEntryTemp, zoom
                )
                val sources = zoom.getAllSource()
                val classFile = processingEnv.filer.createSourceFile(
                    "${packageElement}.ui_list.$className", *sources.toTypedArray()
                )
                classFile.openWriter().use {
                    it.write(content)
                    it.flush()
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
                    zoom.addHolderEntry(getHolder(roundEnvironment, typeElement))
                    processPackageInfo(roundEnvironment, typeElement)
                }

                "BindClickEvent" -> {
                    zoom.addClickEvent(getEvent(roundEnvironment, BindClickEvent::class.java))
                }

                "BindLongClickEvent" -> {
                    zoom.addLongClick(getEvent(roundEnvironment, BindLongClickEvent::class.java))
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
            """.trimIndent().insertCode(viewHolderContent.yes())
        }.joinToString("\n")
        return """
            public static AbstractViewHolder<?> buildFor${entry.itemHolderName}(ViewGroup view, String type) {
                $1
                return null;
            }
            """.trimIndent().insertCode(viewHolderBuilderContent.yes())
    }

    private fun createClassFileContent(
        packageOf: PackageElement, holderEntry: List<Entry<Element>>, zoom: UIListHolderZoom<Element>
    ): String {
        val importHolders = zoom.importHolders()
        val importReceiverClass = zoom.importReceiverClass()
        val javaGenerator = JavaGenerator()
        val buildAddFunction = javaGenerator.buildAddFunction(holderEntry)
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
            package $packageOf.ui_list;

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
            """.trimInsertCode(
            importComposeLibrary.no(),
            importHolders.no(),
            importReceiverClass.no(),
            importComposeRelatedLibrary.no(),
            buildViewHolder.yes(),
            buildAddFunction.yes()
        )
    }

    private fun getEvent(
        roundEnvironment: RoundEnvironment?, clazz: Class<out Annotation>
    ): Map<String, Map<String, List<Event<Element>>>> {
        val eventAnnotations = roundEnvironment?.getElementsAnnotatedWith(clazz).orEmpty()
        val eventMap = eventAnnotations.doubleLayerGroupBy({ element ->
            val viewName = if (clazz.simpleName == "BindClickEvent") element.getAnnotation(BindClickEvent::class.java).viewName
            else element.getAnnotation(BindLongClickEvent::class.java).viewName
            getAnnotationFirstClassArgument(element, clazz)?.let {
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
            s.subInBrackets().split(",").filter { it.isNotBlank() && it.isNotEmpty() }
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

    private fun getAnnotationFirstClassArgument(element: Element, annotation: Class<out Annotation>): Pair<String, String>? {
        val firstOrNull = element.annotationMirrors.firstOrNull {
            it.annotationType.toString() == annotation.canonicalName
        }?.elementValues?.values?.firstOrNull()

        return firstOrNull?.value?.let { list ->
            val fullName = list.toString()
            val simpleName = getSimpleName(fullName)
            fullName to simpleName
        }
    }

    /**
     * 建立item holder 与view holder 的关系
     */
    private fun getHolder(
        roundEnvironment: RoundEnvironment?, typeElement: TypeElement
    ): List<Entry<Element>> {
        val holderAnnotations = roundEnvironment?.getElementsAnnotatedWithAny(typeElement).orEmpty()
        val holderEntry = holderAnnotations.mapNotNull { element ->
            val annotation = element.getAnnotation(BindItemHolder::class.java)
            val type = annotation.type

            val (itemHolderNameFullName, itemHolder) = getAnnotationFirstClassArgument(element, BindItemHolder::class.java) ?: return@mapNotNull null
            element.enclosedElements?.map { it.asType().toString() }?.firstOrNull { it.contains("(") }?.let {
                val bindingFullName = it.subInBrackets()
                val bindingName = getSimpleName(bindingFullName)
                val viewHolderName = element.simpleName.toString()
                val viewHolderFullName = element.asType().toString()

                /**
                 * 根据type 分组
                 */
                val viewHolders = mutableMapOf(type to Holder(bindingName, bindingFullName, viewHolderName, viewHolderFullName))
                Entry(itemHolder, itemHolderNameFullName, viewHolders, element)
            }
        }
        return holderEntry
    }

    private fun String.subInBrackets() = substring(indexOf("(") + 1, lastIndexOf(")"))

    private fun getSimpleName(bindingFullName: String) = bindingFullName.substring(bindingFullName.lastIndexOf(".") + 1)

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
            """.trimInsertCode(
            buildComposeClickListener(eventList).yes(2),
            buildComposeClickListener(eventList2).yes(2)
        )
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
        val buildClickListener = buildInvokeClickEvent(eventMapClick, eventMapLongClick)
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
            produceClickBlockForCompose(e, parameterList)
        }
        """
            if (s == "${it.key}") {
                $1                
            }//if end
        """.trimInsertCode(clickBlock.yes())
    }.joinToString("\n")

    private fun produceClickBlockForCompose(e: Event<Element>, parameterList: String): String {
        return if (e.receiver.contains("Activity"))
            """
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

    private fun buildInvokeClickEvent(event: Map<String, List<Event<Element>>>, event2: Map<String, List<Event<Element>>>): String {
        val singleClickListener = event.map(::produceClickListener).joinToString("\n")
        val longClickListener = event2.map(::produceLongClickListener).joinToString("\n")
        return singleClickListener + longClickListener
    }

    private fun produceClickListener(it: Map.Entry<String, List<Event<Element>>>) = """
            inflate.${it.key}.setOnClickListener((v) -> {
                $1
            });
        """.trimInsertCode(buildInvokeClickEvent(it.value).yes())

    private fun produceLongClickListener(it: Map.Entry<String, List<Event<Element>>>) = """
            inflate.${it.key}.setOnLongClickListener((v) -> {
                $1
                return true;
            });
        """.trimInsertCode(buildInvokeClickEvent(it.value).yes())

    private fun buildInvokeClickEvent(events: List<Event<Element>>): String {
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
}
