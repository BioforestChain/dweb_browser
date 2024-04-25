## cli

plaoc 前后端打包工具。

## 安装

```bash
npm i -g @plaoc/cli
```

## 打包app (bundle/build)

```bash
plaoc bundle ./dir
```
会打包成以下的文件夹结构，并输出压缩文件 `.zip` 和一个 `metadata.json`,详情请查看下面文档详情。

- bundle
  - appId.version.zip
  - metadata.json

### 选项

- `--out`: 指定打包完的目录名称，默认为`bundle`。
- `--version`: 指定app的版本，能覆盖`manifest.json`里面的配置。
- `--id`: 指定app的id，能覆盖`manifest.json`里面的配置。
- `--dir`：用来指定开发目录，即指定您创建`manifest.json`的根目录。
### 示例

```bash
plaoc bundle  ./plaoc/demo/dist --dir ./plaoc/demo --version 0.0.2
```

## 开发者模式 （serve/preview）

需要搭配开发者工具使用，这也是一个app的预览模式。

```bash
plaoc serve ./dir
```

### 选项

- `--dir`：用来指定开发目录，即指定您创建`manifest.json`的根目录。
- `--port`: 用来指定启动的服务端口。
- `--mode`: 服务的处理模式，可以输入`www`,`live`,`prod`。
  - `--mode www`: 将文件夹作为 usr/www 的只读文件进行启动。
  - `--mode live`: 将本地文件夹使用动态服务器进行启, usr/www 会存在一个 index.html 中来进行跳转。
  - `--mode prod`: 对将打包后的文件直接进行服务启动。

### 示例

```bash
plaoc serve  ./plaoc/demo/dist --dir ./plaoc/demo --mode www 
```