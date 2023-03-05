# SML

## 使用

导入插件

```kts
id("com.storyteller_f.sml")
```

加入Source Set

```kts
val smls = listOf("sml_res_colors", "sml_res_dimens", "sml_res_drawables")
val p = smls.map {
    "build/generated/$it/debug"
}
kotlin {
    sourceSets {
        getByName("main") {
            kotlin.srcDirs(p.toTypedArray())
        }
    }
}
```

使用

```kts
interface DD {
    val rectRadius
            : Dimension
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
```