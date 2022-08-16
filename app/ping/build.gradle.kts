import com.storyteller_f.sml.Line
import com.storyteller_f.sml.Oval
import com.storyteller_f.sml.Rectangle
import com.storyteller_f.sml.Ring
import common_ui_list_structure_preset.*
import org.gradle.kotlin.dsl.android

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.storyteller_f.sml")
    id ("com.google.devtools.ksp")
}
android {

    defaultConfig {
        applicationId = "com.storyteller_f.ping"
    }
}

dependencies {
    navigationDependency()
    unitTestDependency()
    implementation("com.google.android.exoplayer:exoplayer-core:2.18.1")
}
baseApp()
setupGeneric()
setupDataBinding()
setupDipToPx()

sml {
    color.set(mutableMapOf("test" to "#ff0000"))
    dimen.set(mutableMapOf("test1" to "12"))
    drawables {
        register("hello") {
            Rectangle {
                solid("#00ff00")
                corners("12dp")
            }
        }
        register("test") {
            Oval {
                solid("#ff0000")
            }
        }
        register("test1") {
            Ring("10dp", "1dp") {
                ring("#ffff00", "10dp")
            }
        }
        register("test2") {
            Line {
                line("#ff0000", "10dp")
            }
        }
    }
}