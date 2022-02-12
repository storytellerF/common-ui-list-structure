package com.storyteller_f.annotation_defination

import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class BindItemHolder(val kProperty1: KClass<out Any>)