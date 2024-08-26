# Dweb Browser

<img align="right" src="./assets/browser-icons/web.browser.dweb.svg" height="150px" alt="跨平台分布式应用浏览器">

![stable-release](https://img.shields.io/badge/dweb-browser-success)
![stable-release](https://img.shields.io/badge/dweb-plaoc-orange)
![starts](https://shields.io/github/stars/BioforestChain/dweb_browser)
[![Plaoc Group][plaoc-badge]][plaoc-url]

[plaoc-badge]: https://img.shields.io/badge/plaoc-doc-blue
[plaoc-url]: https://github.com/BioforestChain/dweb_browser-docs

[ENGLISH DOC](./README_EN.md)

dweb-browser 是一个遵循 Dweb 标准构建起来的浏览器平台，并将浏览器的能力、以及浏览器相关的系统原生系统能力通过各种 dweb 模块暴露出来。目前我们实现了 Android、IOS、MacOS、Windows 这些主流平台的支持。

我们将安装到 `Dweb Browser`中的 app 定义为`dweb app`。通过简单的一个配置文件，就可以将您的 `web app`快速的安装到各个平台当中。
如何开始开发：[流程](https://docs.dweb-browser.org/plaoc/flow.html)。

> - `dweb app`应用开发者文档：[docs](https://docs.dweb-browser.org/)
> - [桌面端下载地址](https://docs.dweb-browser.org/downloads.html)请下载对应平台的版本。
> - 移动端到各大应用商城搜索`Dweb Browser`下载使用。

## `dweb` 模块

在 `Dweb Browser` 中，我们强调模块的概念，所有的一切功能都是`dweb`模块。
一个 `dweb app` 可以小到是一个简单功能模块，去专心为分布式网络提供一些能力,也就是说，你本地调用的模块，可以是远程模块。

模块化的概念将在可编程后端才会有更近一步的体现，模块概念对前端开发者弱化，前端开发者只需要什么功能就简单的调用什么插件。
提供多梯度的开发需求。

模块包含 `std.dweb`,`sys.dweb`,`browser.dweb`，`*.dweb`这些标准。

### `std.dweb`

全平台标准模块，提供的接口需要在各个平台保持一致性。并且需要获得社区认可。

目前有以下三个：

- `file.std.dweb`: 文件操作标准模块，提供操作文件夹的标准能力。
- `http.std.dweb`: http 服务标准模块。
- `dns.std.dweb`: dns 转发查找模块。

### `sys.dweb`

系统级模块，提供一些系统级能力，各个平台的能力可能不一致。
当一些模块在全平台接口保持一致，并且获得社区认可之后，将升级为全平台标准模块。

一些例子：

- `haptics.sys.dweb`: 震动模块，提供震动能力，只在移动端实现。
- `keychain.sys.dweb`: 密钥存储模块。
- `geolocation.sys.dweb`: 获取经纬度的模块。
- ...

### `browser.dweb`

浏览器级模块，实现一些浏览器级的功能。

一些例子：

- `download.browser.dweb`: 下载模块，提供下载能力。
- `jmm.browser.dweb`: 它是一个动态 dweb 模块管理器，基于此可以实现类似 PWA 的应用功能。
- `js.browser.dweb`: 它是一个 javascript-runtime，使用的是 WebWorker 作为底层实现。因此 WebWorker 中的各种标准都可以开箱即用。
- ...

# Plaoc

plaoc 是基于 `Dweb Browser` 平台的一个对标 Cordova、Capacitor、Tauri 的“跨平台 Web 应用”开发工具包。
提供全链路的工具，并且能够快速的调用各个平台和系统的能力。
包含打包工具`@plaoc/cli`，前端插件`@plaoc/plugins`。

## `@plaoc/cli`

[@plaoc/cli](https://www.npmjs.com/package/@plaoc/cli) 是 `dweb app` 开发并打包应用到 `Dweb Browser` 的命令行工具。

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
0: 	dweb://install?url=http://127.0.0.1:8097/metadata.json
1: 	dweb://install?url=http://172.30.95.93:8097/metadata.json
[? Enter the corresponding number to generate a QR code. (0) ›
```

第一行的 `using metadata file`将指定您的 app 配置文件目录，方便直接知晓是哪个 app。

第二行和第三行的`metadata`为 `deeplink` 的形式，输入前面的序号如 0，则生成相对应的二维码，在桌面端可以直接粘贴到 dweb-browser 中进行安装。
而移动端使用扫码的形式进行安装应用。

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

## `@plaoc/plugins`

[@plaoc/plugins](https://www.npmjs.com/package/@plaoc/plugins) 能赋予 web 开发者,直接调用各个平台系统 API 的能力。

具体文档查看：[plugins 文档](https://docs.dweb-browser.org/plugins/web-components.html)

### Q&A

mac 桌面端如果出现： “Dweb Browser” 已损坏，无法打开。 你应该将它移到废纸篓。

可以使用下面命令运行。

```bash
sudo xattr -d com.apple.quarantine /Applications/Dweb\ Browser.app
```

更多问题可以查看文档[dweb_browser](https://docs.dweb-browser.org/)，或者在 issuse 中找到问题或者提问。
