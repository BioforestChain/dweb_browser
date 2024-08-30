# @plaoc/cli

`dweb app` 打包工具，通过此工具打包后，app 就能在任意平台的 `Dweb Browser` 中安装中。

## 安装

```bash
npm i -g @plaoc/cli
```

> 也可以使用 npx plaoc 执行命令

假设工程目录如下所示：

```bash
  plaoc-app
  ├── dist // 工程化编译完的源码目录
  ├── ......其他工程文件
  ├── manifest.json
  └── plaoc.json
```

## 开发模式

### 监听源码变更

```bash
plaoc live ./dist
```

此命令将在指定的文件夹中启动 http 服务，并且根据生成的 http 地址生成`dweb app`的安装地址。
由于绑定的是 http 服务地址，因此 app 安装的时候仅仅是提供转发，并不会将源代码安装到`dweb_browser`中。

简单来说就是在源码文件更新的时候，不用重新安装`dweb app`源代码也跟着更新。

### 选项

- `--port`或 `-p`: 用来指定启动的服务端口。默认为 8096 端口。
- `--config-dir` 或 `-c`: 动态指定配置文件目录，即指定您创建`manifest.json`的根目录。默认使用当前目录。
- `--web-server` 或 `-s`:用来指定 `dweb app` 后端地址。
- `--static-port` 或 `-p2`: 指定静态服务地址。

### 示例

注意，必须指定源码文件夹。

```bash
plaoc live  ./dist
```

输出类似如下：

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

### 监听服务

`plaoc serve` 提供两种模式，可以从静态源码中创建静态服务。也可以指定动态启动的 http 地址来创建安装服务。

#### 指定动态地址

比如使用 `vite --host` 创建的动态服务，这种方式的好处是每次修改代码不用重新安装 app。
但是需要保证设备间能访问到对方，所以如果不是本地可以尽量使用私有地址。

```bash
plaoc serve http://172.30.95.105:5173/
```

输出内容更上面一样。

#### 指定静态源码

指定静态源码安装后，相当于已经安装到`Dweb Browser` 中，不需要安装服务一直启动，但是如果代码发生修改需要重新安装。

```bash
plaoc serve ./dist
```

输出内容更上面一样。

#### 选项

- `--port`或 `-p`: 用来指定启动的服务端口。默认为 8096 端口。
- `--config-dir` 或 `-c`: 动态指定配置文件目录，即指定您创建`manifest.json`的根目录。默认使用当前目录。
- `--web-server` 或 `-s`:用来指定 `dweb app` 后端地址。

## 打包 `dweb app`

`plaoc bundle` 是用来发布 `dweb app` 的时候使用，会打包成以下的文件夹结构，并输出压缩文件 `.zip` 和一个 `metadata.json`。

    |- bundle
      |- appId.version.zip
      |- metadata.json

这里还是指定源码文件夹。

```bash
plaoc bundle ./dist
```

`./dist`目录为您打包的源码目录。并且您需要确保您当前运行 plaoc 命令的文件夹跟您的`manifest.json`文件夹同级。

如果不在同一目录，可以参考下面的 `-c` 目录进行指定。

### 指定`manifest.json`目录

如果您的`manifest.json`跟打包的目录不在同一文件夹下，可以使用 `-c` 指定到`manifest.json`文件夹下。

假设工程目录如下所示：

```bash
  plaoc-main
  ├── ......其他工程文件
  ├── plaoc-app1
    ├── ./dist  //项目打包完的源码文件
    ├── manifest.json
  ├── plaoc-app2
    ├── ./dist  //项目打包完的源码文件
    └── manifest.json
```

假设您目录下有多个项目，就可以像下面这样指定目录去打包。

```bash
plaoc bundle ./plaoc-app1/dist -c ./plaoc-app1
```

> ps： 您也可以使用`plaoc bundle --help` 查看。

### 指定输出打包输出的文件位置

可以使用 `--out` 指定输出目录名称，默认为`bundle`。

```bash
plaoc bundle ./dist --out ./bundleDir
```

#### 指定输出的 appId

可以使用 `--id` 指定 app 的 id。

```bash
plaoc bundle ./dist --id new.plaoc.org.dweb
```

注意指定的 id 需要以 `.dweb` 结尾，并且和配置的 `home` 相同域名。

### 指定输出的 app 版本

可以使用 `--version` 指定 app 的版本。

```bash
plaoc bundle ./dist --version 0.2.3
```

### 选项

- `-o` 或 `--out`: 指定打包完的目录名称，默认名称为`bundle`。
- `-v` 或 `--version`: 指定 app 的版本，能覆盖`manifest.json`里面的配置。
- `--id`: 指定 app 的 id，能覆盖`manifest.json`里面的配置。
- `-c ` 或 `--config-dir`：用来指定开发目录，即指定您创建`manifest.json`的根目录。
- `--clear`: 是否清空编译的文件夹，默认清空。

### 示例

```bash
plaoc bundle  ./dist --dir ./plaoc/demo --version 0.0.2
```

打包完成可以部署到任意可访问的位置，就可以在任意平台的`Dweb Browser`中，访问`metadata.json`文件，就能进行安装了。
