package com.storyteller_f.slim_ktx


fun String.prependRest(indent: String = "    "): String = lineSequence().mapIndexed { index, it ->
    if (index == 0) it
    else when {
        it.isBlank() -> {
            when {
                it.length < indent.length -> indent
                else -> it
            }
        }

        else -> indent + it
    }
}.joinToString("\n")

fun String.insertCode(vararg codeBlock: CodeBlock): String {
    return codeBlock.foldIndexed(this) { i, acc, block ->
        acc.replace("$${i + 1}", block.prependRest())
    }
}

fun String.trimInsertCode(vararg codeBlock: CodeBlock) = trimIndent().insertCode(*codeBlock)

class CodeBlock(private val content: String, val indent: Int) {
    fun prependRest(): String {
        var result = content
        repeat(indent) {
            result = result.prependRest()
        }
        return result
    }
}

fun String.no() = CodeBlock(this, 0)

fun String.indent1(i: Int = 1) = CodeBlock(this, i)
