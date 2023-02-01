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
