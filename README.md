<br />
<p align="center">
<!-- <h1>Dweb-Browser</h1> -->
<img src="./desktop-dev/logo.png" width="240">
</p>

[ENGLISH DOC](./README_EN.md)

![stable-release](https://img.shields.io/badge/dweb-browser-success)
![stable-release](https://img.shields.io/badge/dweb-plaoc-orange)
![starts](https://shields.io/github/stars/BioforestChain/dweb_browser)
[![Plaoc Group][plaoc-badge]][plaoc-url]

[plaoc-badge]: https://img.shields.io/badge/plaoc-doc-blue
[plaoc-url]: https://github.com/BioforestChain/dweb_browser-docs

# About dweb-browser

dweb-browser 是一个遵循 Dweb 标准构建起来的浏览器平台，并将浏览器的能力、以及浏览器相关的系统原生系统能力通过各种 dweb 模块暴露出来。目前我们实现了 Android、IOS、MacOS、Windows、Linux 这些主流平台的支持。
它包含一下这些核心模块：

### `js.browser.dweb`

它是一个 javascript-runtime，使用的是 WebWorker 作为底层实现。因此 WebWorker 中的各种标准都可以开箱即用。

### `jmm.browser.dweb`

它是一个动态 dweb 模块管理器，基于此可以实现类似 PWA 的应用功能

### `mwebview.browser.dweb`

它的全称是 mutil-webview（多 web 视图）的渲染器，可以使用这个渲染器同时渲染多个 Web 视图。比如说可以用它实现一个网页浏览器。

### `desk.browser.dweb`

它是一个 dweb-browser 自己实现的窗口标准 `window.std.dweb`，在跨平台上保持一致性的窗口体验。

### `*.sys.dweb`

和浏览器相关的一些系统标准也在 dweb-browser 上被实现。

## 什么是 plaoc

plaoc 是基于 dweb-browser 平台的一个对标 Cordova、Capacitor、Tauri 的“跨平台 Web 应用”开发工具包

### cli

[@plaoc/cli](https://www.npmjs.com/package/@plaoc/cli) 是 plaoc 开发并打包应用到 dweb_browser 的命令行工具。

1. `npm i -g @plaoc/cli`

1. `plaoc bundle ./dir`
   会打包成以下的文件夹结构，并输出压缩文件 `.zip` 和一个 `plaoc.metadata.json`

1. `plaoc preview http://localhost:1231` 或者 `plaoc preview ./dir`
   > 会将该 url 放在一个 iframe 中被预览
   > 该命令会输出一行命令：
   ```bash
   dweb-browser-dev install --url http://172.30.90.240:8080/usr/metadata.json
   ```

### plugins

[@plaoc/plugins](https://www.npmjs.com/package/@plaoc/plugins) 能赋予 web 开发者,直接调用各个平台系统 API 的能力。

具体文档查看：[plugins 文档](https://docs.dweb-browser.org/plaoc/)

### Q&A

mac 桌面端如果出现： “Dweb Browser” 已损坏，无法打开。 你应该将它移到废纸篓。

可以使用下面命令运行。

```bash
sudo xattr -d com.apple.quarantine /Applications/Dweb\ Browser.app
```

更多问题可以查看文档[dweb_browser](https://docs.dweb-browser.org/)
