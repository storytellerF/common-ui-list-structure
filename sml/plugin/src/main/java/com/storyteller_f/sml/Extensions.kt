package com.storyteller_f.sml

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Project
import java.io.File

fun Project.androidComponents(configure: Action<ApplicationAndroidComponentsExtension>): Unit =
    (this as org.gradle.api.plugins.ExtensionAware).extensions.configure("androidComponents", configure)

fun Project.kotlin(configure: Action<org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension>): Unit =
    (this as org.gradle.api.plugins.ExtensionAware).extensions.configure("kotlin", configure)

fun File.writeXlmWithTags(body: String) {
    ("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<resources>" +
            "$body\n" +
            "</resources>")
        .also { resXml ->
            try {
                createNewFile()
                writeText(resXml)
            } catch (e: Exception) {
                throw GradleException(e.message ?: "")
            }
        }
}