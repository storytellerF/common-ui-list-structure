# giant-explorer plugin core

## 说明

用于开发giant-explorer 的插件。

如果开发的是Web 插件， 不需要引入任何library。API会通过注入的方式加入到页面中。可以使用的api 包括`WebViewFilePlugin`，`DefaultPluginManager`

如果需要读取文件

```javascript
window.addEventListener("message", onMessage);

function onMessage(e) {
    new ImgTouchCanvas({
        canvas: document.getElementById('mycanvas'),
        path: "data:image/png;base64," + e.data,
    });
}
```

其中`e.data` 就是文件内容。



如果是APK 类型的插件，会使用content provider，所以此过程是透明的。

如果是Java 类型的插件，可以使用`GiantExplorerPluginManager`