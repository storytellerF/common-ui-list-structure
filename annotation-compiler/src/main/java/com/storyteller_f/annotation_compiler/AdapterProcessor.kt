package com.storyteller_f.annotation_compiler

import com.storyteller_f.annotation_defination.BindClickEvent
import com.storyteller_f.annotation_defination.BindItemHolder
import com.storyteller_f.annotation_defination.BindLongClickEvent
import java.rmi.activation.UnknownObjectException
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.PackageElement
import javax.lang.model.element.TypeElement

class Entry(
    val viewHolderName: String,
    val bindingName: String,
    val bindingFullName: String,
    val itemHolderName: String,
    val viewHolderFullName: String,
    val itemHolderFullName: String,
    val origin: Element
) {
    override fun toString(): String {
        return "Entry(viewHolderName='$viewHolderName', bindingName='$bindingName', bindingFullName='$bindingFullName', itemHolderName=$itemHolderName)"
    }
}

class Event(
    val receiver: String,
    val receiverFullName: String,
    val functionName: String,
    val parameterList: String,
    val origin: Element
) {
    override fun toString(): String {
        return "Event(receiver='$receiver', functionName='$functionName', parameterCount=$parameterList)"
    }
}

class AdapterProcessor : AbstractProcessor() {

    companion object {
        const val className = "Temp"
    }

    private val setTemp = mutableMapOf<TypeElement, PackageElement>()
    private val holderEntryTemp = mutableListOf<Entry>()
    private val clickEventMapTemp = mutableMapOf<String?, Map<String, List<Event>>>()
    private val longClickEventMapTemp = mutableMapOf<String?, Map<String, List<Event>>>()
    private var count = 0

    @Suppress("NewApi")
    override fun process(
        set: MutableSet<out TypeElement>?,
        roundEnvironment: RoundEnvironment?
    ): Boolean {
        count++
        println("binding event map ${clickEventMapTemp.size} ${longClickEventMapTemp.size} ${set?.size} ${holderEntryTemp.size} ${roundEnvironment?.errorRaised()} ${roundEnvironment?.processingOver()} count $count")

        set?.forEach { typeElement ->
            val name = typeElement.simpleName.toString()
            println(name)
            when (name) {
                "BindItemHolder" -> {
                    getHolder(roundEnvironment, typeElement)?.let { list ->
                        holderEntryTemp.addAll(
                            list
                        )
                    }
                    roundEnvironment?.getElementsAnnotatedWithAny(
                        typeElement
                    )?.let { element ->
                        element.map { processingEnv.elementUtils.getPackageOf(it) }.minByOrNull {
                            it.toString()
                        }?.let {
                            typeElement to it
                        }
                    }?.let { it1 -> setTemp.put(it1.first, it1.second) }
                }
                "BindClickEvent" -> {
                    getEvent(
                        roundEnvironment,
                        BindClickEvent::class.java
                    )?.let { map -> clickEventMapTemp.putAll(map) }
                }
                "BindLongClickEvent" -> {
                    getEvent(
                        roundEnvironment,
                        BindLongClickEvent::class.java
                    )?.let { map -> longClickEventMapTemp.putAll(map) }
                }
            }
        }

        roundEnvironment?.let { environment ->
            if (environment.processingOver()) {
                println("binding event map process: ${this.clickEventMapTemp.size} ${this.longClickEventMapTemp.size} ${holderEntryTemp.size} ${setTemp.size}")
                this.setTemp.forEach { (_, packageElement) ->
                    val content =
                        createClassFileContent(
                            packageElement,
                            this.holderEntryTemp,
                            this.clickEventMapTemp,
                            this.longClickEventMapTemp
                        )
                    val flatMap = clickEventMapTemp.flatMap { entry ->
                        entry.value.flatMap { it.value }.map { it.origin }
                    }.plus(longClickEventMapTemp.flatMap { entry ->
                        entry.value.flatMap { it.value }.map { it.origin }
                    }).plus(holderEntryTemp.map { it.origin })
                    val classFile =
                        processingEnv.filer.createSourceFile(
                            "${packageElement}.adapter_produce.$className",
                            *flatMap.toTypedArray()
                        )
                    classFile.openWriter().use {
                        it.write(content)
                        it.flush()
                    }
                }
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
            val parameterList = element.asType().toString().let { s ->
                val start = s.indexOf("(") + 1
                val end = s.lastIndexOf(")")
                s.substring(start, end).split(",")
                    .filter { it.isNotBlank() && it.isNotEmpty() }
            }.joinToString(", ") {
                when {
                    it.isEmpty() -> ""
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
                parameterList,
                element
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
                        itemHolderName.first,
                        element
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
        return mutableSetOf(
            BindItemHolder::class.java.canonicalName,
            BindClickEvent::class.java.canonicalName,
            BindLongClickEvent::class.java.canonicalName
        )
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
                            "\n             return true;\n        });\n"
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