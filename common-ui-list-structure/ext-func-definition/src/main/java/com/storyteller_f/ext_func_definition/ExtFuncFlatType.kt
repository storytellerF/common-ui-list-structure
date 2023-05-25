package com.storyteller_f.ext_func_definition

enum class ExtFuncFlatType {
    V2,//fragment view
    V3,//fragment view view_binding
    V4,//int to float
    V5,//axx pxx
    V6,//combineDao
    V7,//Dao
}

annotation class ExtFuncFlat(val type: ExtFuncFlatType = ExtFuncFlatType.V2, val isContextReceiver: Boolean = false)