# 工程中运行 plugin/demo 应用流程

- `deno task assets --dev` 开启监听
  编译 dweb_browser/desktop-dev
- `deno task dnt`
  deno 代码会被转换成 nodejs 代码，并输出在 electron 目录下，完成 commonjs 的编译，之后会启动 electron。
  已经做了 source-map 的映射，可以在 deno 代码中直接打断点进行调试（比如使用 vscode）
- `deno task pkg`
  打包 electron 应用
