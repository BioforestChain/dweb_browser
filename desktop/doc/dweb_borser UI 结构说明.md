# 概述
- BrowserWindow 代表一个应用 APP
  - APP 包含
    - 一个第三方 ***.dweb app (webview 标在载入的)
    - 0个或者多个 内置 ***.dweb app (webview 标签载入的，例如： download)
    - 多个 plugin （有UI 的通过 iframe 载入的 例如： status-bar）
      - 一个 BrowserWindow 中只有一套 plugin
    - 多个 webview 会堆叠在一起，后面开启的在最上面；
- Webview 代表一个 dweb (dweb-app)
- UI 结构 模拟 Android



# 相关名词
- ***.dweb 表示一个 符合 dweb 规范的 app