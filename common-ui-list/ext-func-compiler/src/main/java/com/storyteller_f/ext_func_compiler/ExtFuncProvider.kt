package com.storyteller_f.ext_func_compiler

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class ExtFuncProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        val logger = environment.logger
        // error 及以上的会导致注解退出，谨慎使用
        return ExtFuncProcessor(environment.codeGenerator, logger)
    }
}
