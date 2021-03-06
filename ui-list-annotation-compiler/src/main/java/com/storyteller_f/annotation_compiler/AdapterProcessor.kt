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

class Holder(
    val bindingName: String,
    val bindingFullName: String,
    val viewHolderName: String,
    val viewHolderFullName: String
)

class Entry(
    val itemHolderName: String,
    val itemHolderFullName: String,
    val viewHolders: MutableMap<String, Holder>,
    val origin: Element
)

class Event(
    val receiver: String,
    val receiverFullName: String,
    val functionName: String,
    val parameterList: String,
    val key: String,
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
        println(
            "binding event map ${clickEventMapTemp.size} ${longClickEventMapTemp.size} ${set?.size} ${holderEntryTemp.size} " +
                    "${roundEnvironment?.errorRaised()} ${roundEnvironment?.processingOver()} count $count"
        )

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
                    val groupBy = this.holderEntryTemp.groupBy {
                        it.itemHolderFullName
                    }.map { entry ->
                        val first = entry.value.first()
                        entry.value.subList(1, entry.value.size).map {
                            first.viewHolders.putAll(it.viewHolders)
                        }
                        first
                    }
                    val content =
                        createClassFileContent(
                            packageElement,
                            groupBy,
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

    private fun createMultiViewHolder(entry: Entry, eventMapClick: Map<String, List<Event>>, eventMapLongClick: Map<String, List<Event>>): String {
        val viewHolderBuilderContent = entry.viewHolders.map {
            val viewHolderContent = if (it.value.bindingName.endsWith("Binding"))
                buildViewHolder(it.value, eventMapClick, eventMapLongClick)
            else buildComposeViewHolder(it.value, eventMapClick, eventMapLongClick)
            """if (type.equals("${it.key}")) {
${viewHolderContent.prependIndent()}
}//type if end
"""
        }.joinToString("\n")
        return """public static AbstractViewHolder<?> buildFor${entry.itemHolderName}(ViewGroup view, String type) {
${viewHolderBuilderContent.prependIndent()}
    return null;
}
"""
    }

    private fun createClassFileContent(
        packageOf: PackageElement,
        holderEntry: List<Entry>?,
        singleClickEventMap: Map<String?, Map<String, List<Event>>>?,
        longClickEventMap: Map<String?, Map<String, List<Event>>>?
    ): String {
        val importBindingClass = importBindingClass(holderEntry ?: listOf())
        val importReceiverClass = importReceiverClass(singleClickEventMap, longClickEventMap)
        val buildAddFunction = buildAddFunction(holderEntry ?: listOf())
        val hasComposeView = holderEntry?.any { entry ->
            entry.viewHolders.any {
                !it.value.bindingName.endsWith("Binding")
            }
        } ?: true
        val buildViewHolder = holderEntry?.joinToString("\n") {
            val eventMapClick = singleClickEventMap?.get(it.itemHolderFullName) ?: mapOf()
            val eventMapLongClick = longClickEventMap?.get(it.itemHolderFullName) ?: mapOf()
            createMultiViewHolder(it, eventMapClick, eventMapLongClick)
        }

        val importComposeLibrary = if (hasComposeView)
            "import androidx.compose.ui.platform.ComposeView;\n"
        else ""
        val importComposeRelatedLibrary = if (hasComposeView)
            """import com.storyteller_f.view_holder_compose.EDComposeView;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;"""
        else ""
        return """package $packageOf.adapter_produce;

import static com.storyteller_f.ui_list.core.AdapterKt.getList;
import static com.storyteller_f.ui_list.core.AdapterKt.getRegisterCenter;
import android.content.Context;
import com.storyteller_f.ui_list.core.AbstractViewHolder;
import android.view.LayoutInflater;
import android.view.ViewGroup;

$importComposeLibrary
$importBindingClass
$importReceiverClass
import com.storyteller_f.ui_list.event.ViewJava;
$importComposeRelatedLibrary
/**
 * @author storyteller_f
 */
public class $className {
${buildViewHolder?.prependIndent()}
${buildAddFunction.prependIndent()}
}
"""
    }

    private fun getEvent(
        roundEnvironment: RoundEnvironment?,
        clazz: Class<out Annotation>
    ): Map<String?, Map<String, List<Event>>>? {
        val eventAnnotations =
            roundEnvironment?.getElementsAnnotatedWith(clazz)
        val eventMap = eventAnnotations?.splitKeyGroupBy({ element ->
            val viewName = if (clazz.simpleName == "BindClickEvent") element.getAnnotation(BindClickEvent::class.java).viewName
            else element.getAnnotation(BindLongClickEvent::class.java).viewName
            val let = element.annotationMirrors.first()?.elementValues?.map {
                it.value
            }?.let { list ->
                list.first()?.value?.toString() to viewName
            }
            let
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
            val key = if (clazz.simpleName == "BindClickEvent") element.getAnnotation(BindClickEvent::class.java).key
            else element.getAnnotation(BindLongClickEvent::class.java).key
            Event(
                element.enclosingElement.simpleName.toString(),
                element.enclosingElement.toString(),
                element.simpleName.toString(),
                parameterList,
                key,
                element
            )
        }
        return eventMap
    }

    /**
     * ??????item holder ???view holder ?????????
     */
    private fun getHolder(
        roundEnvironment: RoundEnvironment?,
        typeElement: TypeElement
    ): List<Entry>? {
        val holderAnnotations = roundEnvironment?.getElementsAnnotatedWithAny(typeElement)
        val holderEntry = holderAnnotations?.mapNotNull { element ->
            val type = element.getAnnotation(BindItemHolder::class.java).type
            val (itemHolderNameFullName, itemHolder) = element.enclosedElements?.last()?.toString()?.let {
                val start = it.indexOf("(")
                val end = it.indexOf(")")
                val full = it.subSequence(start + 1, end).toString()
                val second = full.lastIndexOf(".")
                full to full.substring(second + 1)
            } ?: return@mapNotNull null
            element.enclosedElements?.map { it.asType().toString() }
                ?.firstOrNull { it.contains("(") }?.let {
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

    private fun importReceiverClass(
        eventMap: Map<String?, Map<String, List<Event>>>?,
        longClickEvent: Map<String?, Map<String, List<Event>>>?
    ): String {
        val flatMap = receiverList(eventMap)
        val flatMap2 = receiverList(longClickEvent)
        return flatMap.plus(flatMap2).joinToString("\n") {
            "import $it;\n"
        }
    }

    private fun receiverList(longClickEvent: Map<String?, Map<String, List<Event>>>?) =
        longClickEvent?.flatMap { it.value.flatMap { entry -> entry.value } }
            ?.map { it.receiverFullName }
            ?.distinct() ?: listOf()

    private fun buildComposeViewHolder(
        it: Holder,
        eventList: Map<String, List<Event>>,
        eventList2: Map<String, List<Event>>
    ): String {
        return """Context context = view.getContext();
EDComposeView composeView = new EDComposeView(context);
ComposeView v = composeView.getComposeView();
${it.viewHolderName} viewHolder = new ${it.viewHolderName}(composeView);
composeView.setClickListener(new Function1<String, Unit>() {
    @Override
    public Unit invoke(String s) {
${buildComposeClickListener(eventList).prependIndent().prependIndent()}
        return null;
    }
});
composeView.setLongClickListener(new Function1<String, Unit>() {
    @Override
    public Unit invoke(String s) {
${buildComposeClickListener(eventList2).prependIndent().prependIndent()}
        return null;
    }
});
return viewHolder;
"""
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
        entry: Holder,
        eventMapClick: Map<String, List<Event>>,
        eventMapLongClick: Map<String, List<Event>>
    ): String {
        val buildClickListener = buildClickListener(eventMapClick, eventMapLongClick)
        return """Context context = view.getContext();
${entry.bindingName} inflate = ${entry.bindingName}.inflate(LayoutInflater.from(context), view, false);

${entry.viewHolderName} viewHolder = new ${entry.viewHolderName}(inflate);
$buildClickListener       
return viewHolder;
"""
    }

    private fun buildComposeClickListener(event: Map<String, List<Event>>) = event.map {
        val clickBlock = it.value.joinToString("\n") { e ->
            val parameterList = parameterList(e.parameterList)
            if (e.receiver.contains("Activity"))
                """if("${e.key}".equals(viewHolder.keyed)) ViewJava.doWhenIs(context, ${e.receiver}.class, (activity) -> {
    activity.${e.functionName}($parameterList);
    return null;//activity return
});//activity end
"""
            else
                """if("${e.key}".equals(viewHolder.keyed)) ViewJava.findActionReceiverOrNull(composeView.getComposeView(), ${e.receiver}.class, (fragment) -> {
    fragment.${e.functionName}($parameterList);
    return null;//fragment return
});//fragment end
"""
        }
        """if (s == "${it.key}") {
${clickBlock.prependIndent()}                
}//if end
"""
    }.joinToString("\n")

    private fun buildClickListener(event: Map<String, List<Event>>, event2: Map<String, List<Event>>): String {
        val singleClickListener = event.map {
            """inflate.${it.key}.setOnClickListener((v) -> {
${buildClickListener(it.value).prependIndent()}
});
"""
        }.joinToString("\n")
        val longClickListener = event2.map {
            """inflate.${it.key}.setOnLongClickListener((v) -> {
${buildClickListener(it.value).prependIndent()}
    return true;
});
"""
        }.joinToString("\n")
        return singleClickListener + longClickListener
    }

    private fun buildClickListener(events: List<Event>): String {
        return events.joinToString("\n") { event ->
            val parameterList = parameterList(
                event.parameterList
            )
            if (event.receiver.contains("Activity")) {
                """if("${event.key}" == viewHolder.keyed) ViewJava.doWhenIs(context, ${event.receiver}.class, (activity)->{
    activity.${event.functionName}($parameterList);
    return null;
});"""
            } else {
                """if("${event.key}" == viewHolder.keyed) ViewJava.findActionReceiverOrNull(v, ${event.receiver}.class, (fragment) -> {
    fragment.${event.functionName}($parameterList);
    return null;
});"""
            }
        }
    }

    private fun parameterList(parameterCount: String): String {
        return parameterCount
    }

    private fun importBindingClass(entries: List<Entry>): String {
        return entries.joinToString("\n") { entry ->
            val allViewHolderModel = entry.viewHolders.map {
                """import ${it.value.bindingFullName};
import ${it.value.viewHolderFullName};"""
            }.joinToString("\n")
            allViewHolderModel + "\nimport ${entry.itemHolderFullName};"
        }
    }

    private fun buildAddFunction(entry: List<Entry>): String {
        var index = 0
        val addFunctions = entry.joinToString("\n") {
            """getRegisterCenter().put(${it.itemHolderName}.class, ${index++});
getList().add($className::buildFor${it.itemHolderName});
"""
        }
        return """public static void add() {
${addFunctions.prependIndent()}   
}
"""
    }
}

inline fun <T, K1, K2, V> Iterable<T>.splitKeyGroupBy(keySelector: (T) -> Pair<K1, K2>?, valueTransform: (T) -> V): Map<K1, Map<K2, List<V>>> {
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