package com.storyteller_f.sml

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.TestExtension
import com.android.build.gradle.api.BaseVariant
import org.gradle.api.Action
import org.gradle.api.DomainObjectSet
import org.gradle.api.GradleException
import org.gradle.api.Project
import java.io.File

fun Project.android(): BaseExtension {
    val android = project.extensions.findByType(BaseExtension::class.java)
    if (android != null) {
        return android
    } else {
        throw GradleException("Project $name is not an Android project")
    }
}

fun Project.androidComponents(configure: Action<ApplicationAndroidComponentsExtension>): Unit =
    (this as org.gradle.api.plugins.ExtensionAware).extensions.configure("androidComponents", configure)

fun BaseExtension.variants(): DomainObjectSet<out BaseVariant> {
    return when (this) {
        is AppExtension -> applicationVariants

        is LibraryExtension -> libraryVariants

        is TestExtension -> applicationVariants
        else -> throw GradleException("Unsupported BaseExtension type!")
    }
}

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