package com.example.ext_func_definition

enum class ExtFuncFlatType {
    v2,//fragment view
    v3,//fragment view view_binding
    v4,//int to float
}

annotation class ExtFuncFlat(val type: ExtFuncFlatType = ExtFuncFlatType.v2, val isContextReceiver: Boolean = false)