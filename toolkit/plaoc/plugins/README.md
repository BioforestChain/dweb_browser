# @plaoc/plugins

plaoc 是基于 dweb-browser 平台的一个对标 Cordova、Capacitor、Tauri 的“跨平台 Web 应用”开发工具包。

运行在 Web 中的前端代码，提供了与后端通讯的 API 调用接口，以及更进一步封装的 WebComponent 组件。

[Plaoc 开发文档](https://docs.dweb-browser.org/plaoc/)

## 开发规范

注意,修改插件需要同步修改文档[dweb_browser-docs](https://github.com/BioforestChain/dweb_browser-docs)。

### 函数注释规范

```
  /**
   * 保存图片到相册
   * @param options
   * @returns boolean
   * @since 2.0.0
   * @Platform android/ios only
   */
```
