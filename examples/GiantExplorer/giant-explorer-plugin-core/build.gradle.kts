import com.android.build.gradle.internal.tasks.factory.dependsOn
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toUpperCaseAsciiOnly
import com.storyteller_f.version_manager.*

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.storyteller_f.version_manager")
}
android {
    namespace = "com.storyteller_f.plugin_core"

    defaultConfig {
        minSdk = 21
    }

}
baseLibrary()

dependencies {

    implementation("androidx.core:core-ktx:${Versions.coreVersion}")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:${Versions.materialVersion}")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
val target = listOf("yue", "li")
val dispatchTasks = target.map { targetName ->
    tasks.register("dispatchPluginCoreLibTo${targetName.toUpperCaseAsciiOnly()}", Copy::class) {
        from(File(buildDir, "/outputs/aar/plugin-core-debug.aar")) {
            rename {
                it.replace("plugin-core-debug.aar", "core.aar")
            }
        }
        into(File(rootDir, "../../giant-explorer/$targetName/giant-explorer-plugin-core"))
    }
}

afterEvaluate {
    val taskName = "bundleDebugAar"
    val packageDebug = tasks.findByName(taskName)
    dispatchTasks.forEach {
        it.dependsOn(taskName)
        packageDebug?.finalizedBy(it)
    }
}