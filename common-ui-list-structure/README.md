一个Android 软件架构

整合了jetpack paging，view Model，navigation，compose

特殊模块
file-system：封装android 文件系统，无视不同安卓版本。待解决document provider，root access
ui-list,ui-list-annotation-compiler，ui-list-annotation-definition: 封装jetpack paging ，paging adapter

sml 自动生成color， drawable的xml 文件

使用的代码类似：
```kotlin
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
```

示例模块

giant-explorer 文件管理器

ping 动态壁纸

fapiao 发票管理

## 代码规范

1. 禁止使用DataBinding 进行数据绑定和逻辑代码。xml 本身就有很高的冗余，在xml 中写“代码”只会增加维护成本。
2. DataItemHolder 应该继承自添加了`ItemHolder` 注解的接口。未来会通过ksp 严格限制。

## 创建一个app

```kotlin
import common_ui_list_structure_preset.*

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-android")
    id("kotlin-parcelize")
}

android {
    defaultConfig {
        applicationId = "com.storyteller_f.fapiao"
    }
    namespace = "com.storyteller_f.fapiao"
}

dependencies {
    implementation(fileTree("libs"))
    implementation(project(":fapiao-reader"))
    //根据需要添加
    networkDependency()
    navigationDependency()
    unitTestDependency()
    fileSystemDependency()
    workerDependency()

    implementation("com.tom-roush:pdfbox-android:2.0.25.0")
}
//必须添加
baseApp()
//推荐添加
setupGeneric()
setupDataBinding()
setupDipToPx()
```