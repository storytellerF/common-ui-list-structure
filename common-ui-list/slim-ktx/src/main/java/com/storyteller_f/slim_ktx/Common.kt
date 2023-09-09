package com.storyteller_f.slim_ktx

/**
 * 添加intent，除了第一行
 */
fun String.indentRest(indent: String = "    "): String = lineSequence().mapIndexed { index, element ->
    when {
        index == 0 -> element
        element.isNotBlank() -> indent + element
        element.length < indent.length -> indent
        else -> element
    }
}.joinToString("\n")

/**
 * 将指定位置的内容替换成code block。
 */
fun String.insertCode(vararg codeBlock: CodeBlock): String {
    return codeBlock.foldIndexed(this) { i, acc, block ->
        acc.replace("$${i + 1}", block.indentRest())
    }
}

fun String.trimInsertCode(vararg codeBlock: CodeBlock) = trimIndent().insertCode(*codeBlock)

class CodeBlock(private val content: String, private val indent: Int) {
    /**
     * 根据指定intent 添加intent
     */
    fun indentRest(): String {
        var result = content
        repeat(indent) {
            result = result.indentRest()
        }
        return result
    }
}

/**
 * 不需要添加intent
 */
fun String.no() = CodeBlock(this, 0)

/**
 * 添加指定的intent
 */
fun String.yes(i: Int = 1) = CodeBlock(this, i)
