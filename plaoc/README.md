# About plaoc

> [Engish doc](./README_US.md)

[plaoc](https://github.com/BioforestChain/dweb_browser-docs) 是基于 dweb-browser 平台的一个对标 Cordova、Capacitor、Tauri 的“跨平台 Web 应用”开发工具包。

## cli

[plaoc cli 文档](./cli/README.md)

### dev Build

到 dweb_browser 根目录。

```bash
deno task plaoc serve ./plaoc/demo/dist
```

> `./plaoc/demo/dist` 指向一个 build 之后的应用目录

## MutilWebview概念

1. 在 Dweb Browser 中，MutilWebview 作为前端载具，没有对全局变量的污染，所有的扩展都是通过网络通讯来实现功能（fetch/websocket）。
2. 在 Dweb Browser 中，JsProcess 作为后端的载具，可以跟各个模块直接 IPC 通讯而不通过传统意义上的网络层。
3. plugins 这个文件夹，就是在 JsProcess 这个环境里，对这些接口进行了聚合整理，使得开发者开箱即用地能够使用这些接口来访问各个模块。也就是说，它分成两部分的代码：
   1. JsProcess 的后端代码，通过`jsProcess.nativeFetch/.nativeRequest`来访问其它模块，从而使得开发者可以将它挂特定的 subdomain/port 里，使得前端开发者可以访问这些模块。
   2. MutilWebview 的前端代码，对后端的网络请求进行进一步封装，简化成 WebComponent 的声明式模块，使得接口更加符合前端开发者的思维直觉。
      > 额外地，我们会基于这些 WebComponent 做一个进一步的封装，使得能够尽可能兼容 capacitor、cordova 等传统前端应用开发框架

## publish

- 打标签，然后推送到github 

```bash
git tag -a 0.0.1 -m "feat: xxx"
```

```bash
git push origin <tag-name>
```
