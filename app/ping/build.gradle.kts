import com.storyteller_f.sml.*
import common_ui_list_structure_preset.*
import org.gradle.kotlin.dsl.android
import kotlin.reflect.full.declaredMemberProperties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.storyteller_f.sml")
    id("com.google.devtools.ksp")
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

interface DD {
    val rectRadius: Dimension
}

class Test : DD {
    override val rectRadius: Dimension
        get() = Dp(12f)
}

sml {
    color.set(mutableMapOf("test" to "#ff0000"))
    dimen.set(Test::class.dimens())
    drawables {
        register("hello") {
            Rectangle {
                solid("#00ff00")
                corners(Test::rectRadius.reference())
            }
        }
        register("test") {
            Oval {
                solid("#ff0000")
            }
        }
        register("test1") {
            Ring("10dp", "1dp") {
                ring("#ffff00", Dp(10f))
            }
        }
        register("test2") {
            Line {
                line("#ff0000", Dp(10f))
            }
        }
    }
}