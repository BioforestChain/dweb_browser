## 2024-3

1.  KMP/Desktop（JVM）
    1. 开始了基于 JVM/AWT/Swing/JxBrowser/JCEF 技术的桌面端开发
    1. 完成了 helperPlatform、pureImage、core、window、sys、dwebview 等模块
    1. 实现了 libs 模块对 macos、windows 平台的动态链接库的编译支持

## 2024-2

1. KMP 多平台
   1. 新增 search.browser.dweb 模块，提供搜索引擎和搜索关键字历史记录等
   1. 适配了通用折叠屏标准
   1. 升级了地理位置模块（location.sys.dweb）的标准
1. KMP/IOS
   1. 适配地理位置模块（geolocation.sys.dweb）的实现
   1. 优化二进制包内存布局，提升启动速度
   1. 修复了 dwebview 存在内存泄露的问题
   1. 浏览器应用（web.browser.dweb）优化动画；优化状态更新；修复交互冲突；
1. ploac
   1. 为生物识别插件的检测支持提供了更多样的返回
   1. 更新了定位插件的标准

## 2024-1

1. KMP 多平台
   1. 实现 multipart.http.std.dweb，使用 rust-ffi 实现核心流式解析
   1. 完成对数字媒体录制模块（media-capture.sys.dweb）模块的初步设计与开发。
   1. 完成对快捷方式模块（shortcut.sys.dweb）模块的初步设计与开发。
   1. 完成对联系人选择器模块（contact-picker.sys.dweb）模块的初步设计与开发。
   1. 改进 pureImage 模块，提供了双引擎只能切换
   1. 修复了 ipc 通讯管理释放不够干净导致的问题
   1. 完善权限管理（permission.sys.dweb）的相关开发
      1. 同时适配一些应用市场的上架需求
      1. 优化了列表界面展示
1. KMP/IOS
   1. 项目架构升级，从 xcproject 管理方式升级成 xcworkspace
   1. 浏览器应用（web.browser.dweb）
      1. 改进暗色模式的支持
      1. 重写 WebCacheStore 和 WebCache，减少重绘和数据订阅更新
      1. 修复标签关闭时的资源释放问题
      1. 修复窗口拖动时的性能问题
   1. 字体和尺寸缩放优化
   1. 修复 WindowBottomSheetModal 没有被正确随着释放而关闭的问题
   1. 修复 jmm 动态模块升级丢失数据的问题
1. KMP/Android
   1. 原生浏览器应用（web.browser.dweb）新增搜索引擎自动化配置
   1. 模块管理（jmm.browser.dweb）优化状态同步机制；改进历史列表展示
   1. 下载管理（download.browser.dweb）优化状态同步机制
   1. 修复后置摄像头取消拍照导致崩溃的问题
   1. 修复窗口与虚拟键盘的互操作性
   1. 新增 apk 应用自动升级（临时方案）
1. ploac
   1. 重构 扫码插件，修复了 IOS 平台上 video 存在系统控件等问题
   1. 完成 快捷方式模块的 插件化对接
   1. cli 新增 init 指令用于快速创建项目配置文件
1. docs
   1. 迁移和更新文档，从`VuePress`迁移到`VitePress`，更新插件文档
1. KMP/Desktop（Electron）
   > 尝试将应用迁移到 Electron 这个 js 平台上，
   > 在经过一个半月技术验证后，目前已经放弃该路径，
   > 但探索过程中遗留下来的架构层面的技术资产继续保留。
   > 3 月份开始走 JVM 路线开发桌面端。
   1. 架构改进，现有 platform*、helper*、pure*、lib* 系列的模块
   1. 完成 pureHttp 的抽象定义，兼容现有平台的实现
   1. 完成 pureImage 的实现改进，引入 Coil-3.alpha
   1. 初步完成 pureCrypto 的定义与实现

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
