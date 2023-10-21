# 文件管理器

## 功能

1. 插件
2. 访问document provider
3. 暴露出一个content provider
4. 访问ftp，sftp，ftps，smb，webdav 资源
5. root 访问

关于第三条，本APP 会暴露一个content provider，
但是这个uri 对应的资源可以是另一个APP 提供的content provider，
虽然这听起来很棒，但是在某些手机上有可能无法正常工作，并且原有的授权还会失效。
即使在某些某些手机上可以使用，也无法正常使用**canRead**（这原本是用来鉴权的，当前并不会影响使用）。