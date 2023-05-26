# 工程中运行 plugin/demo 应用流程
- 编译 demo 在 dweb_browser 终端运行 deno task dev 就能够实现 plugin demo 打包到 ./electron/assets/cot-demo 目录
- 编译 dweb_browser/desktop-dev 终端运行： deno task assets [--dev]开启监听
- 启动electron dweb_browsre/desktop-dev  终端运行: deno task dnt 