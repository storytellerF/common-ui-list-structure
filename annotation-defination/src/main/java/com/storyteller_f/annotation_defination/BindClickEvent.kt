package com.storyteller_f.annotation_defination

import kotlin.reflect.KClass

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
annotation class BindClickEvent(val kClass: KClass<out Any>, val viewName: String = "getRoot()")

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
annotation class BindLongClickEvent(val kClass: KClass<out Any>, val viewName: String = "getRoot()")