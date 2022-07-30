package com.storyteller_f.sml

import org.junit.Test

class Test1 {
    @Test
    fun checkRect() {
        val rectangleShapeDrawable = RectangleShapeDrawable(dither = true, tint = "test", optionalInsetLeft = "0dp", optionalInsetRight = "0dp")
        rectangleShapeDrawable.stroke("#ff0000", "12dp")
        rectangleShapeDrawable.padding("10dp", "10dp", "10dp", "10dp")
        rectangleShapeDrawable.size("10dp", "10dp")
        rectangleShapeDrawable.solid("#ff00ae")
        rectangleShapeDrawable.corners("16dp")
        rectangleShapeDrawable.linearGradient("#ff0000", "#00ff00")
        val output = rectangleShapeDrawable.output()
        val trimIndent = """
<?xml version="1.0" encoding="utf-8"?>
<shape android:shape="rectangle"
    dither="true"
    visible="true"
    tint="test"
    
    optionalInsetLeft="0dp"
    optionalInsetRight="0dp"
    
    xmlns:android="http://schemas.android.com/apk/res/android">
    <stroke android:color="#ff0000" android:width="12dp"/>
    <padding android:top="10dp" android:right="10dp" android:left="10dp" android:bottom="10dp"/>
    <size android:width="10dp" android:height="10dp"/>
    <solid android:color="#ff00ae"/>
    <corners android:radius="16dp"/>
    <gradient android:type="linear" 
        android:endColor="#00ff00"
        android:startColor="#ff0000"
        android:useLevel="false"
        android:angle="0.0"/>
</shape>""".trimIndent()
        assert(output == trimIndent)
    }

    @Test
    fun testRing() {
        val ringShapeDrawable = RingShapeDrawable("10dp", "10", false)
        ringShapeDrawable.start()
        ringShapeDrawable.ring("#ff0000", "10dp")
        println(ringShapeDrawable.output())
        assert(ringShapeDrawable.output() == """
<?xml version="1.0" encoding="utf-8"?>
<shape android:shape="ring"
    dither="false"
    visible="true"
    android:innerRadius="10dp"
    android:thickness="10"
    
    
    xmlns:android="http://schemas.android.com/apk/res/android">
    <stroke android:color="#ff0000" android:width="10dp"/>
</shape>
        """.trimIndent())
    }
}