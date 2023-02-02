# BFS 桌面版

目前以该版本为标准展开其它平台的翻译工作。

## 运行此 DEMO

1. 安装依赖
   ```shell
   npm insall
   ```
1. 编译代码（开发模式）
   1. 启用 ts 编译
      ```shell
      tsc -w
      ```
   1. 打包`js-process.worker`的代码
      ```shell
      npm run bundle:js-process.worker --watch
      ```
   1. 打包`desktop.worker`的代码
      ```shell
      npm run bundle:desktop.worker --watch
      ```
1. 下载 [nwjs sdk](https://nwjs.io/downloads/)
1. 然后执行：
   ```shell
   cd $BFS_REPO_PATH/desktop
   $NWJS_PATH/nw.exe ./
   ```

## 源码阅读辅助

1. 这里由 cts 与 mts 后缀的文件，其中 .mts 是给 web 用的，以 esmodule 为标准，省去 bundle 的逻辑
1. worker.cts 是给 WebWorker 环境用的，因为我们启动用的 WebWorker 默认没有启动 esmodule，所以需要通过 esbuild 进行打包。
1. 启动有两个入口，一个是 index.html 是提供一个按钮手动启动，方便打断点后启动调试。一个是 index.js 是直接无窗口启动，是正式发布时的入口。
