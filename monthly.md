## 2024-1 至 2024-2

1. KMP 多平台
   1. 对 DWebViewEngine 的一些 sys 级别接口的标准化加速
   1. 对 DWebViewEngine 中的下载适配到下载管理模块（download.browser.dweb）中统一管理
   1. 继续推进文件模块（file.std.dweb）的标准化
   1. 使用 Rust 实现 multipart.http.std.dweb 模块
   1. 改进底层模块之间的通讯标准与生命周期的管理方案
   1. 推出一种 KMP 与各个原生平台之间数据状态管理的互操作方案
1. KMP/Desktop 平台
   1. 迁移内核模块到 js 与 wasi 平台
   1. 初步完成桌面版的适配

## 2023-12

1. KMP 多平台
   1. 网络模块（http.std.dweb）新增通用通讯接口`/fetch`与`/websocket`
   1. 优化模块管理（jmm.browser.dweb）与下载管理（download.browser.dweb）的视图层写作，提升用户体验
   1. 改进 DWebViewEngine 的 dweb-deeplink 的适配方案
1. KMP/Android 平台
   1. 适配地理位置模块（geolocation.sys.dweb）的实现
1. KMP/IOS 平台
   1. 将原生浏览器应用（web.browser.dweb）的数据读写切换成 KMP 所提供的标准接口
   1. 将原生浏览器应用（web.browser.dweb）的 WebView 替换成 KMP 所提供的 DWebViewEngine 标准
   1. 增加手势返回，适配 KMP 平台统一的接口
   1. 提升应用启动速度、优化动画、修复内存管理、修复手势冲突

## 2023-11

1. KMP 多平台
   1. 迁移实现了下载管理模块（download.browser.dweb）。
   1. 迁移实现了桌面与窗口模块（desk.browser.dweb）。
   1. 迁移实现了 JS 进程管理模块（js-process.browser.dweb）
1. KMP/Android 平台
   1. 修复软键盘的适配问题
   1. 改进 WebView 的截图触发机制，提升网页性能
1. KMP/IOS 平台
   1. 提供了 biometrics, device、barcode-scanning 等基础 sys 级别的插件。
   1. 完善 IOS 与 KMP 的通讯桥
   1. 适配主题切换
   1. 继续改善原生视图与 Compose 视图的融合

## 2023-10

1. KMP 多平台
   1. 优化了多语言的适配。
   1. 完成对权限管理（permission.std.dweb）模块的初步设计与开发。
   1. 优化对网络模块（http.std.dweb）接口的设计，使得更加契合底层网络协议标准。
   1. 重构域名管理模块（dns.std.dweb），使之支持更加灵活的子协议与可编程的路由。
   1. 在窗口标准中新增了 抽屉视图（bottomsheet） 的窗口标准，使得模块之间的窗口视图的互动更加灵活。
   1. 通过 Rust 实现 zip 解压缩，以支持流模式。
1. KMP/IOS 平台
   1. 对 iOS17 WKWebView 进行了研究，以实现对 https 代理响应。
   1. 修复了原生视图与 Compose 视图的一些同步问题。
   1. 提供了 clipboard, config, haptics 等基础 sys 级别的插件。
