package com.storyteller_f.annotation_compiler

import com.storyteller_f.annotation_defination.BindClickEvent
import com.storyteller_f.annotation_defination.BindLongClickEvent
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
    val parameterCount: Int
) {
    override fun toString(): String {
        return "Event(receiver='$receiver', functionName='$functionName', parameterCount=$parameterCount)"
    }
}

class AdapterProcessor : AbstractProcessor() {
    private var count = 0

    @Suppress("NewApi")
    override fun process(
        set: MutableSet<out TypeElement>?,
        roundEnvironment: RoundEnvironment?
    ): Boolean {
        count++
        println("set:$set is over ${roundEnvironment?.processingOver()} count:$count")
        val eventMap = getEvent(roundEnvironment, BindClickEvent::class.java)
        val longClickEventMap = getEvent(roundEnvironment, BindLongClickEvent::class.java)
        set?.forEach { typeElement ->
            val packageOf = processingEnv.elementUtils.getPackageOf(
                roundEnvironment?.getElementsAnnotatedWithAny(typeElement)?.firstOrNull()
            )
            val holderEntry = getHolder(roundEnvironment, typeElement)
            val content =
                createClassFileContent(packageOf, holderEntry, eventMap, longClickEventMap)
            val createClassFile =
                processingEnv.filer.createSourceFile("${packageOf}.adapter_produce.Temp")
            createClassFile.openWriter().use {
                it.write(content)
                it.flush()
            }
        }
        return false
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
        val buildViewHolder = holderEntry?.joinToString("\n") {
            val eventList = eventMap?.get(it.itemHolderName) ?: mapOf()
            val eventList2 = longClickEventMap?.get(it.itemHolderName) ?: mapOf()
            if (it.bindingName.endsWith("Binding"))
                buildViewHolder(it, eventList, eventList2)
            else
                buildComposeViewHolder(it)
        }

        return "package $packageOf.adapter_produce;\n" +
                "import static com.storyteller_f.ui_list.core.SimpleSourceAdapterKt.getList;\n" +
                "import static com.storyteller_f.ui_list.core.SimpleSourceAdapterKt.getRegisterCenter;\n" +
                "import android.content.Context;\n" +
                "import android.view.LayoutInflater;\n" +
                "import android.view.ViewGroup;\n\n" +
                "import androidx.compose.ui.platform.ComposeView;\n" +
                "\n" +
                importBindingClass + "\n" +
                importReceiverClass + "\n" +
                "import com.storyteller_f.ui_list.event.ViewJava;\n" +
                "\n" +
                "/**\n" +
                " * @author storyteller_f\n" +
                " */\n" +
                "public class Temp {\n" +
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
            }.size
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
            eventMap?.flatMap { it.value.flatMap { entry -> entry.value } }?.map { it.receiverFullName }
                ?.distinct()
        return flatMap?.joinToString("\n") {
            "import $it;\n"
        } ?: ""
    }

    private fun buildComposeViewHolder(it: Entry): String {
        return "    public static ${it.viewHolderName} build${it.viewHolderName}(ViewGroup view) {\n" +
                "        Context context = view.getContext();\n" +
                "        ${it.viewHolderName} viewHolder = new ${it.viewHolderName}(new ComposeView(context));\n" +
                "        return viewHolder;\n" +
                "    }"
    }

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf(
            "com.storyteller_f.annotation_defination.BindItemHolder",
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
                "       Context context = view.getContext();\n" +
                "       ${entry.bindingName} inflate = ${entry.bindingName}.inflate(LayoutInflater.from(context), view, false);\n" +
                "\n" +
                "       ${entry.viewHolderName} viewHolder = new ${entry.viewHolderName}(inflate);\n" +
                buildClickListener(event, eventList2) +
                "       return viewHolder;\n" +
                "   }"
    }

    private fun buildClickListener(
        event: Map<String, List<Event>>,
        event2: Map<String, List<Event>>
    ): String {
        return event.map {
            "       inflate.${it.key}.setOnClickListener((v) -> {\n" +
                    buildClickListener(it.value) +
                    "\n       });\n"
        }.joinToString("\n") + event2.map {
            "       inflate.${it.key}.setOnLongClickListener((v) -> {\n" +
                    buildClickListener(it.value) +
                    "\n       });\n"
        }.joinToString("\n")
    }

    private fun buildClickListener(events: List<Event>): String {
        return events.joinToString("\n") { event ->
            if (event.receiver.contains("Activity")) {
                "           ViewJava.doWhenIs(context, ${event.receiver}.class, (activity)->{\n" +
                        "               activity.${event.functionName}(${parameterList(event.parameterCount)});\n" +
                        "               return null;\n" +
                        "           });"
            } else {
                "           ViewJava.findActionReceiverOrNull(v, ${event.receiver}.class, (fragment) -> {\n" +
                        "               fragment.${event.functionName}(${parameterList(event.parameterCount)});\n" +
                        "               return null;\n" +
                        "           });"
            }
        }
    }

    private fun parameterList(parameterCount: Int): String {
        return "v" +
                if (parameterCount == 2)
                    ", viewHolder.getItemHolder()"
                else ""
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
                            "       getList().add(Temp::build${it.viewHolderName});\n"
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