# 一个Android 软件架构

[![Android CI](https://github.com/storytellerF/common-ui-list-structure/actions/workflows/android.yml/badge.svg)](https://github.com/storytellerF/common-ui-list-structure/actions/workflows/android.yml)
[![Android Plugin CI](https://github.com/storytellerF/common-ui-list-structure/actions/workflows/android-plugins.yml/badge.svg)](https://github.com/storytellerF/common-ui-list-structure/actions/workflows/android-plugins.yml)

整合了Jetpack Paging，View Model，Navigation，Compose

## Build

```shell
# 编译全部
bash build_all.sh
# 仅编译app
bash build_app.sh
# 仅编译插件
bash build_plugins.sh
```

## 特殊模块

1. **file-system**：封装android 文件系统。

2. **ui-list**： 封装**Jetpack Paging**， **PagingAdapter**

3. **ext-func**：自动为Context 的扩展函数生成View，Fragment 的同名扩展函数。

4. **SML**：[自动生成color，drawable的xml 文件](SML)

5. **version-manager**： 使用includeBuild 导入，暴露一个gradle plugin，用于依赖管理和配置管理

## 示例项目

[giant-explorer](examples/GiantExplorer) 文件管理器

[ping](examples/Ping) 动态壁纸

## 代码规范

1. 禁止使用DataBinding 进行数据绑定和逻辑代码，特别是bindingAdapter。xml 本身就有很高的冗余，在xml 中写“代码”只会增加维护成本。
2. DataItemHolder 应该继承自添加了`ItemHolder` 注解的接口。未来会通过ksp 严格限制。
3. 基于状态,而不是事件
4. 唯一数据源

## 创建一个app

```kts
import common_ui_list_structure_preset.*

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-android")
    id("kotlin-parcelize")
    id("com.storyteller_f.version_manager")
}

android {
    defaultConfig {
        applicationId = "com.storyteller_f.**"
    }
    namespace = "com.storyteller_f.**"
}

dependencies {
    implementation(fileTree("libs"))
    //根据需要添加
    networkDependency()
    fileSystemDependency()
    workerDependency()
}
//必须添加
baseApp()
//推荐添加
setupGeneric()
setupDataBinding()
setupDipToPx()
```

定义数据结构

```kotlin
@Entity(tableName = "repos")
data class Repo(
    @PrimaryKey @field:SerializedName("id") val id: Long,
    @field:SerializedName("name") val name: String,
) : Datum<RepoRemoteKey> {
    override fun commonDatumId() = id.toString()
    override fun produceRemoteKey(prevKey: Int?, nextKey: Int?) =
        RepoRemoteKey(commonDatumId(), prevKey, nextKey)

    override fun remoteKeyId(): String = commonDatumId()
}

@Entity(tableName = "repo_remote_keys")
class RepoRemoteKey(
    itemId: String,
    prevKey: Int?,
    nextKey: Int?
) : RemoteKey(itemId, prevKey, nextKey)
```

如果是本地的数据，不必继承自 `Datum`，可以直接继承自 `Model`。Model 不包含remoteKey。

使用数据

```kotlin
//添加一个Adapter
val adapter = SimpleSourceAdapter<FileItemHolder, FileViewHolder>()
//绑定数据到RecycleView
listWithState.sourceUp(adapter, owner, session.selected, flash = ListWithState.Companion::remote)

//定义ViewHolder
class FileItemHolder(
    val file: FileModel,
    val selected: MutableLiveData<MutableList<Pair<DataItemHolder, Int>>>
) : DataItemHolder {
    override fun areItemsTheSame(other: DataItemHolder): Booloan

    override fun areContentsTheSame(other: DataItemHolder): Boolean 
}
@BindItemHolder(FileItemHolder::class)
class FileViewHolder(private val binding: ViewHolderFileBinding) : AdapterViewHolder<FileItemHolder>(binding) {
    override fun bindData(itemHolder: FileItemHolder) = binding.fileName.text = itemHolder.file.name
}
```

更多信息访问[wiki](https://github.com/storytellerF/common-ui-list-structure/wiki)