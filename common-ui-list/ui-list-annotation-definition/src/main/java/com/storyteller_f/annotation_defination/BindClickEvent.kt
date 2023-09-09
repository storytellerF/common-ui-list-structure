package com.storyteller_f.annotation_defination

import kotlin.reflect.KClass

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
/**
 * @param key 设置事件时，仅允许指定adapter 中View Holder 的事件
 */
annotation class BindClickEvent(
    val kClass: KClass<out Any>,
    val viewName: String = "getRoot()",
    val key: String = "default"
)

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
annotation class BindLongClickEvent(
    val kClass: KClass<out Any>,
    val viewName: String = "getRoot()",
    val key: String = "default"
)
