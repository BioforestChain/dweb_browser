## 环境安装

> 记得科学上网，否则部分软件下载的速度会非常慢。

1.  **Xcode 安装**：
    AppStroe 下载

1.  **安装 Android Studio**

    - 下载 [Android Studio](https://developer.android.com/studio/) 稳定版
      - 安装安装引导，一路 next + agree
      - License Agreement 记得所有的 license 都点 agree, 否则，无法 next.

1.  **安装 Deno**

    - 给 Mac 用户的建议：
      1. 终端上执行：`curl -fsSL https://deno.land/x/install/install.sh | sh`
      1. 添加 deno 的环境变量。方便下次使用。 环境变量的添加，依赖你所使用终端环境。 比如：使用使用 zprofile 配置。 - a. `vim ~/.zprofile` - b. 添加 `export PATH=$PATH:/Users/bfchainer/.deno/bin` (deno 的安装路径，默认是: /Users/xxx/.deno/bin) - c. `source .zprofile`

1.  **安装 Node 与 包管理器**

    > 建议直接安装 20+

    - 1. `npm i -g pnpm`: 前端包管理器 pnpm，通常工具链的开发会用它
    - 1. `npm i -g yarn`: 前端包管理器 yarn，直接面向工程的开发会用它

1.  **建议安装**
    - [Visual Studio Code](https://code.visualstudio.com/download)
    - [Github Desktop](https://desktop.github.com/)

## 启动项目

1. 在项目目录下的 next/kmp 文件下存放着 DWebBrowser 的多源代码，使用 AndroidStudio 打开。
1. 在项目根目录运行： `deno task dev`，它会自动拉取子仓库，并安装依赖，然后启动开发者模式
   - 所谓开发者模式，就是对一些资源文件做实时的自动化预编译，并实时复制到 iOS, Android，Desktop 等环境下。
   - 如果 deno 长时间运行，可能会出现一些文件监听失效或者子进程异常退出，那么重启再启动 `deno task dev` 即可
1. 编译，运行：
   - 如果是 Android 用户，直接启动模拟器或者 USB 连接开启调试等设备后，可以直接运行
   - 如果是 IOS 用户，确保 `Kotlin Multiplatform Mobile` 插件已经安装，在`Edit Configurations`中，点击`+`，选`IOS Application`
     1. 在面板中，先选 `xcode project file`，路径在 `next/kmp/app/iosApp/iosApp.xcodeproj`
     1. 然后就可以选择 `scheme` 为 `iosApp` ； `configuration` 建议是 `Debug`
     1. 最后是 `target`，可以选模拟器，如果你的真机设备找不到，那么就卸载重装`Kotlin Multiplatform Mobile`插件，然后通常就能选择真机设备了，反正只要找不到，就重装插件就行
     1. 当然，也可以直接用 xcode 打开`next/kmp/app/iosApp/iosApp.xcodeproj`，直接运行即可
