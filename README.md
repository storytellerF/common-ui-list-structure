# 一个Android 软件架构

整合了Jetpack Paging，View Model，Navigation，Compose

## 特殊模块

1. **file-system**：[封装android 文件系统](https://github.com/storytellerF/AFS)

2. **ui-list**： [封装**Jetpack Paging**， **PagingAdapter**](https://github.com/storytellerF/common-ui-list/tree/main/ui-list)

3. **ext-func**：[自动为Context 的扩展函数生成View，Fragment 的同名扩展函数](https://github.com/storytellerF/common-ui-list/tree/main/ext-func-compiler)

4. **SML**：[自动生成color、drawable、dimen的xml 文件](https://github.com/storytellerF/SML)

5. **version-manager**： [使用includeBuild 导入，暴露一个gradle plugin，用于依赖管理和配置管理](https://github.com/storytellerF/common-ui-list/tree/main/version-manager)

6. **Bao**：[兜底异常](https://github.com/storytellerF/Bao)

7. **Yong**：[使用lint 检查未经捕获的异常](https://github.com/storytellerF/Yong)

## 示例项目

[giant-explorer](https://github.com/storytellerF/GiantExplorer) 文件管理器 [![Android CI](https://github.com/storytellerF/GiantExplorer/actions/workflows/android.yml/badge.svg)](https://github.com/storytellerF/GiantExplorer/actions/workflows/android.yml)

[ping](https://github.com/storytellerF/Ping) 动态壁纸 [![Android CI](https://github.com/storytellerF/Ping/actions/workflows/android.yml/badge.svg)](https://github.com/storytellerF/Ping/actions/workflows/android.yml)

## 代码规范

1. 禁止使用DataBinding 进行数据绑定和逻辑代码，特别是bindingAdapter。XML 本身就有很高的冗余，在XML 中写“代码”只会增加维护成本。
2. DataItemHolder 应该继承自添加了`ItemHolder` 注解的接口。
3. 基于状态,而不是事件

    如何数据来源是事件，可以使用*LiveData*, *Flow* 转成状态。状态可以持久化，而事件不可以。

4. 单一来源

    如果确实存在多个来源，可以利用*印章类*，*Optional&lt;T>*， *Flow* 等将不同来源的数据汇集成一个对象。

5. 函数 > 类

    代码实现优先选择函数，基本上就是“**优先组合，而不是继承**”的另一种描述。

    使用函数构造一个实例也会比手动实例化更有优势。

6. 更加通用==更加复杂==更难维护

更多信息访问[wiki](https://github.com/storytellerF/common-ui-list-structure/wiki)
