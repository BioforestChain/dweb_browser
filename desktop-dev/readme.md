# 工程中运行 plugin/demo 应用流程
- 编译 dweb_browser/plugins  终端运行：deno task build:src
- 编译 dweb_browser/example/vue3 终端运行： deno task build:demo
- 把 vue3 变异出来的结果同步到 dweb_browser/desktop-dev/electron/bundle/cot-demo 目录 在终端运行： deno task sync:desktop
- 编译 dweb_browser/desktop-dev 终端运行： deno task assets [--dev]开启监听
- 启动electron dweb_browsre/desktop-dev  终端运行: deno task dnt 