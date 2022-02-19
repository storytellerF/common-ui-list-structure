package com.storyteller_f.annotation_compiler

import com.storyteller_f.annotation_defination.BindClickEvent
import com.storyteller_f.annotation_defination.BindItemHolder
import com.storyteller_f.annotation_defination.BindLongClickEvent
import java.rmi.activation.UnknownObjectException
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.PackageElement
import javax.lang.model.element.TypeElement

class Entry(
    val viewHolderName: String,
    val bindingName: String,
    val bindingFullName: String,
    val itemHolderName: String,
    val viewHolderFullName: String,
    val itemHolderFullName: String
) {
    override fun toString(): String {
        return "Entry(viewHolderName='$viewHolderName', bindingName='$bindingName', bindingFullName='$bindingFullName', itemHolderName=$itemHolderName)"
    }
}

class Event(
    val receiver: String,
    val receiverFullName: String,
    val functionName: String,
    val parameterList: String
) {
    override fun toString(): String {
        return "Event(receiver='$receiver', functionName='$functionName', parameterCount=$parameterList)"
    }
}

class AdapterProcessor : AbstractProcessor() {
    private var count = 0

    companion object {
        const val className = "Temp"
    }

    @Suppress("NewApi")
    override fun process(
        set: MutableSet<out TypeElement>?,
        roundEnvironment: RoundEnvironment?
    ): Boolean {
        count++
        println("binding set:$set is over ${roundEnvironment?.processingOver()} count:$count")
        if (set == null || set.isEmpty()) return false
        val eventMap = getEvent(roundEnvironment, BindClickEvent::class.java)
        val longClickEventMap = getEvent(roundEnvironment, BindLongClickEvent::class.java)
        set.forEach { typeElement ->
            val packageElement = processingEnv.elementUtils.getPackageOf(
                roundEnvironment?.getElementsAnnotatedWithAny(typeElement)?.firstOrNull()
            )
            val holderEntry = getHolder(roundEnvironment, typeElement)
            val content =
                createClassFileContent(packageElement, holderEntry, eventMap, longClickEventMap)
            val classFile =
                processingEnv.filer.createSourceFile("${packageElement}.adapter_produce.$className")
            classFile.openWriter().use {
                it.write(content)
                it.flush()
            }
        }
        return true
    }

    private fun createClassFileContent(
        packageOf: PackageElement,
        holderEntry: List<Entry>?,
        eventMap: Map<String?, Map<String, List<Event>>>?,
        longClickEventMap: Map<String?, Map<String, List<Event>>>?
    ): String {
        val importBindingClass = importBindingClass(holderEntry ?: listOf())
        val importReceiverClass = importReceiverClass(eventMap?.plus(longClickEventMap ?: mapOf()))
        val buildAddFunction = buildAddFunction(holderEntry ?: listOf())
        var hasComposeView = false
        val buildViewHolder = holderEntry?.joinToString("\n") {
            val eventList = eventMap?.get(it.itemHolderName) ?: mapOf()
            val eventList2 = longClickEventMap?.get(it.itemHolderName) ?: mapOf()
            if (it.bindingName.endsWith("Binding"))
                buildViewHolder(it, eventList, eventList2)
            else {
                if (!hasComposeView) hasComposeView = true
                buildComposeViewHolder(it, eventList, eventList2)
            }
        }

        return "package $packageOf.adapter_produce;\n" +
                "import static com.storyteller_f.ui_list.core.SimpleSourceAdapterKt.getList;\n" +
                "import static com.storyteller_f.ui_list.core.SimpleSourceAdapterKt.getRegisterCenter;\n" +
                "import android.content.Context;\n" +
                "import android.view.LayoutInflater;\n" +
                "import android.view.ViewGroup;\n\n" +
                (if (hasComposeView)
                    "import androidx.compose.ui.platform.ComposeView;\n"
                else "") +
                "\n" +
                importBindingClass + "\n" +
                importReceiverClass + "\n" +
                "import com.storyteller_f.ui_list.event.ViewJava;\n" +
                (if (hasComposeView)
                    "import com.storyteller_f.view_holder_compose.EDComposeView;\n\n" +
                            "import kotlin.Unit;\n" +
                            "import kotlin.jvm.functions.Function1;"
                else "") +
                "\n" +
                "/**\n" +
                " * @author storyteller_f\n" +
                " */\n" +
                "public class $className {\n" +
                buildViewHolder + "\n" +
                buildAddFunction + "\n" +
                "}\n"
    }

    private fun getEvent(
        roundEnvironment: RoundEnvironment?,
        clazz: Class<out Annotation>
    ): Map<String?, Map<String, List<Event>>>? {
        val eventAnnotations =
            roundEnvironment?.getElementsAnnotatedWith(clazz)
        val eventMap = eventAnnotations?.splitKeyGroupBy({ element ->
            element.annotationMirrors.first()?.elementValues?.map {
                it.value
            }?.let { list ->
                list.first()?.value?.toString()?.let {
                    it.substring(it.lastIndexOf(".") + 1)
                } to if (list.size == 2) list[1]?.value.toString() else "getRoot()"
            }
        }) { element ->
            val parameterCount = element.asType().toString().let { s ->
                val start = s.indexOf("(")
                val end = s.lastIndexOf(")")
                s.substring(start, end).split(",")
                    .filter { it.isNotBlank() && it.isNotEmpty() }
            }.joinToString(", ") {
                when {
                    it.contains("android.view.View") -> {
                        "v"
                    }
                    it.contains("Holder") -> {
                        "viewHolder.getItemHolder()"
                    }
                    else -> {
                        throw UnknownObjectException(it)
                    }
                }
            }
            Event(
                element.enclosingElement.simpleName.toString(),
                element.enclosingElement.toString(),
                element.simpleName.toString(),
                parameterCount
            )
        }
        return eventMap
    }

    private fun getHolder(
        roundEnvironment: RoundEnvironment?,
        typeElement: TypeElement
    ): List<Entry>? {
        val holderAnnotations = roundEnvironment?.getElementsAnnotatedWithAny(typeElement)
        val holderEntry = holderAnnotations?.mapNotNull { element ->
            val itemHolderName = element.enclosedElements?.last()?.toString()?.let {
                val start = it.indexOf("(")
                val end = it.indexOf(")")
                val full = it.subSequence(start + 1, end).toString()
                val second = full.lastIndexOf(".")
                full to full.substring(second + 1)
            } ?: return@mapNotNull null
            element.enclosedElements?.map { it.asType().toString() }
                ?.firstOrNull { it.contains("(") }?.let {
                    val bindingFullName = it.substring(it.indexOf("(") + 1, it.lastIndexOf(")"))
                    val bindingName =
                        bindingFullName.substring(bindingFullName.lastIndexOf(".") + 1)
                    Entry(
                        element.simpleName.toString(),
                        bindingName,
                        bindingFullName,
                        itemHolderName.second,
                        element.asType().toString(),
                        itemHolderName.first
                    )
                }
        }
        return holderEntry
    }

    private fun importReceiverClass(eventMap: Map<String?, Map<String, List<Event>>>?): String {
        val flatMap =
            eventMap?.flatMap { it.value.flatMap { entry -> entry.value } }
                ?.map { it.receiverFullName }
                ?.distinct()
        return flatMap?.joinToString("\n") {
            "import $it;\n"
        } ?: ""
    }

    private fun buildComposeViewHolder(
        it: Entry,
        eventList: Map<String, List<Event>>,
        eventList2: Map<String, List<Event>>
    ): String {
        return "    public static ${it.viewHolderName} build${it.viewHolderName}(ViewGroup view) {\n" +
                "        Context context = view.getContext();\n" +
                "        EDComposeView composeView = new EDComposeView(context);\n" +
                "        ComposeView v = composeView.getComposeView();\n" +
                "        ${it.viewHolderName} viewHolder = new ${it.viewHolderName}(composeView);\n" +
                "        composeView.setClickListener(new Function1<String, Unit>() {\n" +
                "            @Override\n" +
                "            public Unit invoke(String s) {\n" +
                buildComposeClickListener(eventList) +
                "                return null;\n" +
                "            }\n" +
                "        });\n" +
                "        composeView.setLongClickListener(new Function1<String, Unit>() {\n" +
                "            @Override\n" +
                "            public Unit invoke(String s) {\n" +
                buildComposeClickListener(eventList2) +
                "                return null;\n" +
                "            }\n" +
                "        });\n" +
                "        return viewHolder;\n" +
                "    }\n"
    }

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf(BindItemHolder::class.java.canonicalName)
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latestSupported()
    }

    private fun buildViewHolder(
        entry: Entry,
        event: Map<String, List<Event>>,
        eventList2: Map<String, List<Event>>
    ): String {
        return "    public static ${entry.viewHolderName} build${entry.viewHolderName}(ViewGroup view) {\n" +
                "        Context context = view.getContext();\n" +
                "        ${entry.bindingName} inflate = ${entry.bindingName}.inflate(LayoutInflater.from(context), view, false);\n" +
                "\n" +
                "        ${entry.viewHolderName} viewHolder = new ${entry.viewHolderName}(inflate);\n" +
                buildClickListener(event, eventList2) +
                "        return viewHolder;\n" +
                "   }"
    }

    private fun buildComposeClickListener(event: Map<String, List<Event>>): String {
        return event.map {
            "                if (s == \"${it.key}\") {\n" +
                    it.value.joinToString("\n") { e ->
                        if (e.receiver.contains("Activity"))
                            ("                    ViewJava.doWhenIs(context, ${e.receiver}.class, (activity) -> {\n" +
                                    "                         activity.${e.functionName}(${
                                        parameterList(
                                            e.parameterList
                                        )
                                    });\n" +
                                    "                         return null;//activity return\n" +
                                    "                     });//activity end\n")
                        else
                            ("                    ViewJava.findActionReceiverOrNull(composeView.getComposeView(), ${e.receiver}.class, (fragment) -> {\n" +
                                    "                         fragment.${e.functionName}(${
                                        parameterList(
                                            e.parameterList
                                        )
                                    })\n" +
                                    "                         return null;//fragment return\n" +
                                    "                     });//fragment end\n")
                    } +

                    "                }//if end\n"
        }.joinToString("\n")

    }

    private fun buildClickListener(
        event: Map<String, List<Event>>,
        event2: Map<String, List<Event>>
    ): String {
        return event.map {
            "        inflate.${it.key}.setOnClickListener((v) -> {\n" +
                    buildClickListener(it.value) +
                    "\n        });\n"
        }.joinToString("\n") +
                event2.map {
                    "        inflate.${it.key}.setOnLongClickListener((v) -> {\n" +
                            buildClickListener(it.value) +
                            "\n        });\n"
                }.joinToString("\n")
    }

    private fun buildClickListener(events: List<Event>): String {
        return events.joinToString("\n") { event ->
            if (event.receiver.contains("Activity")) {
                "            ViewJava.doWhenIs(context, ${event.receiver}.class, (activity)->{\n" +
                        "                activity.${event.functionName}(${parameterList(event.parameterList)});\n" +
                        "                return null;\n" +
                        "            });"
            } else {
                "            ViewJava.findActionReceiverOrNull(v, ${event.receiver}.class, (fragment) -> {\n" +
                        "                fragment.${event.functionName}(${parameterList(event.parameterList)});\n" +
                        "                return null;\n" +
                        "            });"
            }
        }
    }

    private fun parameterList(parameterCount: String): String {
        return parameterCount
    }

    private fun importBindingClass(entry: List<Entry>): String {
        return entry.joinToString("\n") {
            "import ${it.bindingFullName};\n" +
                    "import ${it.viewHolderFullName};\n" +
                    "import ${it.itemHolderFullName};"
        }
    }

    private fun buildAddFunction(entry: List<Entry>): String {
        var index = 0
        return "    public static void add() {\n" +
                entry.joinToString("\n") {
                    "       getRegisterCenter().put(${it.itemHolderName}.class, ${index++});\n" +
                            "       getList().add($className::build${it.viewHolderName});\n"
                } +
                "   }\n"
    }
}

inline fun <T, K1, K2, V> Iterable<T>.splitKeyGroupBy(
    keySelector: (T) -> Pair<K1, K2>?,
    valueTransform: (T) -> V
): Map<K1, Map<K2, List<V>>> {
    val destination = mutableMapOf<K1, MutableMap<K2, MutableList<V>>>()
    for (element in this) {
        val key = keySelector(element)
        key?.let {
            val map = destination.getOrPut(key.first) { mutableMapOf() }
            val secondMap = map.getOrPut(key.second) {
                mutableListOf()
            }
            secondMap.add(valueTransform(element))
        }

    }
    return destination
}