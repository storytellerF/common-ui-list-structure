package com.storyteller_f.annotation_defination

import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
/**
 * @param type 允许多个View Holder 绑定一个Item Holder。选择那个View Holder 由Adapter决定。决定方式时字符串相等。Adapter 默认type 为空
 */
annotation class BindItemHolder(val itemHolderClass: KClass<out Any>, val type: String = "")