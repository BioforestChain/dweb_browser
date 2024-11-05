# Dweb Browser

<img align="right" src="./assets/browser-icons/web.browser.dweb.svg" height="150px" alt="跨平台分布式应用浏览器">

![stable-release](https://img.shields.io/badge/dweb-browser-success)
![stable-release](https://img.shields.io/badge/dweb-plaoc-orange)
![starts](https://shields.io/github/stars/BioforestChain/dweb_browser)
[![Plaoc Group][plaoc-badge]][plaoc-url]

[plaoc-badge]: https://img.shields.io/badge/plaoc-doc-blue
[plaoc-url]: https://github.com/BioforestChain/dweb_browser-docs

[ENGLISH DOC](./README.md) ｜ [Welcome to Dweb Browser discord](https://discord.gg/nBPgPzPbgX)

dweb-browser 是一个遵循 Dweb 标准构建起来的浏览器平台，并将浏览器的能力、以及浏览器相关的系统原生系统能力通过各种 dweb 模块暴露出来。目前我们实现了 Android、iOS、MacOS、Windows 这些主流平台的支持。

我们将安装到 `Dweb Browser`中的 app 定义为`dweb app`。通过简单的一个配置文件，就可以将您的 `web app`快速的安装到各个平台当中。
如何开始开发：[流程](https://docs.dweb-browser.org/plaoc/flow.html)。

> - `dweb app`应用开发者文档：[docs](https://docs.dweb-browser.org/)
> - [桌面端下载地址](https://docs.dweb-browser.org/downloads.html)请下载对应平台的版本。
> - 移动端到各大应用商城搜索`Dweb Browser`下载使用。

## `dweb`

在 `Dweb Browser` 中，我们强调模块的概念，所有的一切功能都是`dweb`模块。
一个 `dweb app` 可以小到是一个简单功能模块，去专心为分布式网络提供一些能力,也就是说，你本地调用的模块，可以是远程模块。

模块化的概念将在可编程后端才会有更近一步的体现，模块概念对前端开发者弱化，前端开发者只需要什么功能就简单的调用什么插件。
提供多梯度的开发需求。

模块包含 `std.dweb`,`sys.dweb`,`browser.dweb`，`*.dweb`这些标准。

### `std.dweb`标准

全平台标准模块，提供的接口需要在各个平台保持一致性。并且需要获得社区认可。

目前有以下三个：

- `file.std.dweb`: 文件操作标准模块，提供操作文件夹的标准能力。
- `http.std.dweb`: http 服务标准模块。
- `dns.std.dweb`: dns 转发查找模块。

### `sys.dweb` 标准

系统级模块，提供一些系统级能力，各个平台的能力可能不一致。
当一些模块在全平台接口保持一致，并且获得社区认可之后，将升级为全平台标准模块。

一些例子：

- `haptics.sys.dweb`: 震动模块，提供震动能力，只在移动端实现。
- `keychain.sys.dweb`: 密钥存储模块。
- `geolocation.sys.dweb`: 获取经纬度的模块。
- ...

### `browser.dweb` 标准

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

具体文档查看：[cli 文档](https://docs.dweb-browser.org/plaoc/cli.html)

## `@plaoc/plugins`

[@plaoc/plugins](https://www.npmjs.com/package/@plaoc/plugins) 能赋予 web 开发者,直接调用各个平台系统 API 的能力。

具体文档查看：[plugins 文档](https://docs.dweb-browser.org/plugins/web-components.html)

## 如何贡献代码

1. 查看： [快速开始](./GET_START_FOR_DEVELOPER.quick.md)
2. 建议阅读： [完整的开发环境搭建的文档](./GET_START_FOR_DEVELOPER.md)
3. 初始化资源包

```bash
deno task init
```

### 参与多平台应用的贡献

需要编译`Dweb Browser` 静态资源文件。

```bash
deno task dev
```

### 参与 `Plaoc` 工具包贡献

```bash
deno task plaoc:watch
```

### 示例项目

项目内包含前后端示例项目，位于[examples](./toolkit/plaoc/examples/).

可运行如下命令查看：

#### 前端示例项目

```bash
plaoc:demo
```

#### 包含可编程后端的示例项目

```bash
plaoc:demo:serve
```

### 参与静态库贡献

项目的一些系统级 API 和一些服务采用 [rust-uniffi](https://gitlab.com/trixnity/uniffi-kotlin-multiplatform-bindings)进行调用.

具体查看项目 [dweb_browser_libs](https://github.com/BioforestChain/dweb_browser_libs).
