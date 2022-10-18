import com.storyteller_f.sml.config.*
import com.storyteller_f.sml.dimens
import com.storyteller_f.sml.reference
import common_ui_list_structure_preset.*

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
    namespace = "com.storyteller_f.ping"
}

dependencies {
    implementation("com.google.android.exoplayer:exoplayer-core:2.18.1")
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("com.squareup.okio:okio:3.2.0")
    implementation("com.github.bumptech.glide:glide:4.14.2")
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
                solid(RgbColor("#00ff00"))
                corners(Test::rectRadius.reference())
            }
        }
        register("test") {
            Oval {
                solid(RgbColor("#00ff00"))
            }
        }
        register("test1") {
            Ring("10dp", "1dp") {
                ring(RgbColor("#00ff00"), Dp(10f))
            }
        }
        register("test2") {
            Line {
                line(RgbColor("#00ff00"), Dp(10f))
            }
        }
    }
}