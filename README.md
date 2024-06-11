# Dweb-Browser

<img align="right" src="./assets/browser-icons/web.browser.dweb.svg" height="150px" alt="跨平台分布式应用浏览器">

![stable-release](https://img.shields.io/badge/dweb-browser-success)
![stable-release](https://img.shields.io/badge/dweb-plaoc-orange)
![starts](https://shields.io/github/stars/BioforestChain/dweb_browser)
[![Plaoc Group][plaoc-badge]][plaoc-url]

[plaoc-badge]: https://img.shields.io/badge/plaoc-doc-blue
[plaoc-url]: https://github.com/BioforestChain/dweb_browser-docs

dweb-browser 是一个遵循 Dweb 标准构建起来的浏览器平台，并将浏览器的能力、以及浏览器相关的系统原生系统能力通过各种 dweb 模块暴露出来。目前我们实现了 Android、IOS、MacOS、Windows、Linux 这些主流平台的支持。

- 下载地址： [桌面端](https://docs.dweb-browser.org/downloads.html)，移动端到各大应用商城下载。
- 应用开发者文档：[docs](https://docs.dweb-browser.org/)

[ENGLISH DOC](./README_EN.md)

# Plaoc

plaoc 是基于 dweb-browser 平台的一个对标 Cordova、Capacitor、Tauri 的“跨平台 Web 应用”开发工具包，包含打包工具`@plaoc/cli`，前端插件`@plaoc/plguins`。

## cli

[@plaoc/cli](https://www.npmjs.com/package/@plaoc/cli) 是 plaoc 开发并打包应用到 dweb_browser 的命令行工具。

### 安装打包工具。

```bash
npm i -g @plaoc/cli
```

开发 app 的时候运行` plaoc serve`。

- 第一种方式可以指定您前端工程输出的地址，这样在您代码更新的时候，app 内部的代码也会跟着您的更新而更新。

```bash
plaoc serve http://localhost:8000
```

- 第二种是直接指定编译完的源码目录，这种方式相当于直接安装 app，适用您前端工程断开的时候也能访问。

```bash
plaoc serve ./dist
```

上面的两个命令会输出如下几行：

```bash
using metadata file: /Volumes/developer/waterbang/deno/dweb_browser/npm/@plaoc__examples/html-demo/manifest.json
metadata: 	dweb://install?url=http://127.0.0.1:8097/metadata.json
metadata: 	dweb://install?url=http://172.30.95.93:8097/metadata.json
```

第一行的 `using metadata file`将指定您的 app 配置文件目录，方便直接知晓是哪个 app。

第二行和第三行的`metadata`为 `deeplink` 的形式，在桌面端可以直接粘贴到 dweb-browser 中进行安装。
而移动端可以使用转成二维码，使用扫码的形式进行安装应用。

### 打包成可部署的 app 包

直接使用 `plaoc bundle` 指定源码目录进行打包，命令如下：

```bash
plaoc bundle ./dir
```

会打包并输出一个包含 app ID 和日期组合而成的压缩文件 `.zip` 和一个 `metadata.json`。

这两个文件使用任意的`(http/https)` 服务部署成链接的形式，放于同一文件夹中并且指向`metadata.json` 文件。组成如下形式链接，就可以在的 dweb-browser 中进行安装。

```bash
dweb://install?url=http://app.dweb.中国/metadata.json
```

## plugins

[@plaoc/plugins](https://www.npmjs.com/package/@plaoc/plugins) 能赋予 web 开发者,直接调用各个平台系统 API 的能力。

具体文档查看：[plugins 文档](https://docs.dweb-browser.org/plugins/web-components.html)

## 模块

在 dweb-browser 中，我们强调模块的概念，一个 app 可以小到是一个简单功能模块，去专心为分布式网络提供一些能力，它包含一下这些核心模块：

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

### Q&A

mac 桌面端如果出现： “Dweb Browser” 已损坏，无法打开。 你应该将它移到废纸篓。

可以使用下面命令运行。

```bash
sudo xattr -d com.apple.quarantine /Applications/Dweb\ Browser.app
```

更多问题可以查看文档[dweb_browser](https://docs.dweb-browser.org/)，或者在 issuse 中找到问题或者提问。
