package com.storyteller_f.ext_func_compiler

import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.storyteller_f.slim_ktx.yes

internal fun generatePropertyV5(task: ExtFuncProcessor.Task): Pair<Set<String>, String> {
    val arguments = if (task.ksAnnotated is KSFunctionDeclaration) {
        task.ksAnnotated.parameters.map {
            it.type.element
        }.joinToString(",")
    } else null
    val imports = getImports(task.ksAnnotated) + listOf("androidx.fragment.app.Fragment", "androidx.activity.ComponentActivity")
    return imports to extendVm(arguments, task)
}

private fun extendVm(extra: String?, task: ExtFuncProcessor.Task): String {
    val parameterList = getParameterListExcludeDefaultList(task.ksAnnotated as KSFunctionDeclaration)
    val parameterString = parameterList.joinToString(",\n").yes(3).indentRest()
    val second = parameterList.toMutableList().apply {
        add(1, "vmScope: VMScope")
    }.joinToString(",\n").yes(3).indentRest()
    return """
        //$extra
        @MainThread
        inline fun <reified VM : ViewModel, ARG> Fragment.a${task.name}(
            $parameterString
        ) = ${task.name}(arg, { requireActivity().viewModelStore }, { requireActivity() }, vmProducer)
        @MainThread
        inline fun <reified VM : ViewModel, ARG> Fragment.p${task.name}(
            $parameterString
        ) = ${task.name}(arg, { requireParentFragment().viewModelStore }, { requireParentFragment() }, vmProducer)
        @MainThread
        inline fun <reified VM : ViewModel, T, ARG> T.${task.name}(
            $second
        )  where T : SavedStateRegistryOwner, T : ViewModelStoreOwner = ${task.name}(arg, vmScope.storeProducer, vmScope.ownerProducer, vmProducer)
        """.trimIndent()
}