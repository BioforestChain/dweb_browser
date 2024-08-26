# @plaoc/cli

`dweb app` 打包工具，通过此工具打包后，app 就能在任意平台的 `dweb-browser` 中安装中。

## 安装

```bash
npm i -g @plaoc/cli
```

## 开发模式：监听源码

```bash
plaoc live ./dist
```

此命令将在指定的文件夹中启动 http 服务，并且根据生成的 http 地址生成`dweb app`的安装地址。
由于绑定的是 http 服务地址，因此 app 安装的时候仅仅是提供转发，并不会将源代码安装到`dweb_browser`中。

并且在 dist 文件更新的时候，不用重新安装`dweb app`源代码也跟着更新。

### 选项

- `--port`或 `-p`: 用来指定启动的服务端口。默认为 8096 端口。
- `--config-dir` 或 `-c`: 动态指定配置文件目录，即指定您创建`manifest.json`的根目录。默认使用当前目录。
- `--web-server` 或 `-s`:用来指定 `dweb app` 后端地址。

### 示例

```bash
plaoc live  ./dist
```

输出如下：

```bash
0: dweb://install?url=http://127.0.0.1:8096/metadata.json
1: dweb://install?url=http://172.30.95.105:8096/metadata.json
? Enter the corresponding number to generate a QR code. (0) › 1
▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄
█ ▄▄▄▄▄ █▀▀ ███▄█▀█▀▄ ██  █ ▄▄▄▄▄ █
█ █   █ █▄▀██▀▀▄▄█▄▄█▀██  █ █   █ █
█ █▄▄▄█ █ ▄ █ ██▀█  ▀ █▄▄ █ █▄▄▄█ █
█▄▄▄▄▄▄▄█ █ ▀▄▀▄█▄▀ █▄▀ █ █▄▄▄▄▄▄▄█
█  ▀ ▀█▄▄▀▀█  ▀▄▄ ███▄▄▄█▄█ ▄▄▀ ▄▀█
█ ▄███▄▄█▄▀▀   ▄█ █▀   ▄▀▀█ ▄ ██ ▄█
█▀█▀▄▄ ▄▀▀▀▄ █▀ ▄▄ █ █ ▄▄▄   ▀█▀█ █
█▀ ▄█▀█▄█ ▄▄ █▄▄██ ▀██  ▄▄▀▀█ ▀█▄██
█▄ ▀▄█ ▄▄██▀█  ▀█ ▄▀█▀ ▀ █▀▄█▄▀▀▀▄█
█▄▀▄█▀▀▄ ▄ ▀▄ █ █ ▀██▀▀ ▀▄▄██▄▄█  █
█▄ ▀▄ █▄▀▄█▀▀██▀▄ ▀██▄▄▄▄▀█▀██ ▀▄▀█
█▄ ▀█  ▄▄ █ ███▀█▀█    ▄█ █▀ █ ▄▀ █
█▄████▄▄█▀▀█▀ ██▄  █▀█▄▄█ ▄▄▄ █▀▄ █
█ ▄▄▄▄▄ █▄▄▀█ ▀▀▀█ █▀▄▀ █ █▄█ ▄█ ▀█
█ █   █ █▀▀█▄█▄▄█▀▄█▄▀ ▀█▄▄   ▄█▄██
█ █▄▄▄█ █▀ █▀█ █▀▄▀ ▀ ▀█▄ █▀  ▄██▄█
█▄▄▄▄▄▄▄█▄▄█▄▄█▄████▄█▄▄█▄██▄▄██▄██
```

生成的二维码可以在`Dweb Browser`移动端使用扫码模块进行扫码安装。

## 开发模式：监听服务

`plaoc serve` 提供两种模式，可以从静态源码中创建静态服务。也可以指定动态启动的 http 地址来创建安装服务。

### 指定动态地址

比如使用 `vite --open` 创建的动态服务，这种方式的好处是每次修改代码不用重新安装 app，但是需要保证设备间能访问到对方。

```bash
plaoc serve http://localhost:5173/
```

### 指定静态源码

指定静态源码安装后，相当于已经安装到`Dweb Browser` 中，不需要安装服务一直启动，但是如果代码发生修改需要重新安装。

```bash
plaoc serve ./dist
```

#### 选项

- `--port`或 `-p`: 用来指定启动的服务端口。默认为 8096 端口。
- `--config-dir` 或 `-c`: 动态指定配置文件目录，即指定您创建`manifest.json`的根目录。默认使用当前目录。
- `--web-server` 或 `-s`:用来指定 `dweb app` 后端地址。

## 打包 `dweb app`

`plaoc bundle` 是用来发布 `dweb app` 的时候使用，会打包成以下的文件夹结构，并输出压缩文件 `.zip` 和一个 `metadata.json`。

    |- bundle
      |- appId.version.zip
      |- metadata.json

```bash
plaoc bundle ./dir
```

### 选项

- `--out`: 指定打包完的目录名称，默认名称为`bundle`。
- `--version`: 指定 app 的版本，能覆盖`manifest.json`里面的配置。
- `--id`: 指定 app 的 id，能覆盖`manifest.json`里面的配置。
- `--dir`：用来指定开发目录，即指定您创建`manifest.json`的根目录。
- `--watch`: 打包将文件将动态生成。默认为 true.

### 示例

```bash
plaoc bundle  ./plaoc/demo/dist --dir ./plaoc/demo --version 0.0.2
```
