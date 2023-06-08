package com.example.ui_list_annotation_common

import com.storyteller_f.slim_ktx.trimInsertCode
import com.storyteller_f.slim_ktx.yes
import javax.lang.model.element.Element

abstract class UiAdapterGenerator {
    /**
     * 用于添加到列表中
     */
    abstract fun buildAddFunction(entry: List<Entry<Element>>): String

    companion object {
        const val className = "HolderBuilder"
    }
}

class KotlinGenerator : UiAdapterGenerator() {
    override fun buildAddFunction(entry: List<Entry<Element>>): String {
        TODO("Not yet implemented")
    }

}

class JavaGenerator: UiAdapterGenerator() {
    override fun buildAddFunction(entry: List<Entry<Element>>): String {
        var index = 0
        val addFunctions = entry.joinToString("\n") {
            buildRegisterBlock(it, index++)
        }
        return """
            public static int add(int offset) {
                $1
                return $index;
            }
            """.trimInsertCode(addFunctions.yes())
    }
    private fun buildRegisterBlock(it: Entry<Element>, index: Int) = """
                getRegisterCenter().put(${it.itemHolderName}.class, $index + offset);
                getList().add($className::buildFor${it.itemHolderName});
            """.trimIndent()

}