## 环境安装

> 记得科学上网，否则部分软件下载的速度会非常慢。
>
> Gradle 不建议使用 mirror，因为我们使用的包基本都是官方最新的，第三方 mirror 往往不能及时更新，如果发现`could not resolve all dependencies for configuration`的错误，请检查全局的 gradle 的配置可能错误了。

1.  **安装 Android Studio**

    - 下载 [Android Studio](https://developer.android.com/studio/) 稳定版
      - 安装安装引导，一路 next + agree
      - License Agreement 记得所有的 license 都点 agree, 否则，无法 next.

1.  **Xcode 安装**：

    - AppStore 下载
    - Windows/Linux 或者不编译 IOS 项目，则不需要

1.  **安装 Deno**

    - 给 Mac 用户的建议：
      1. 终端上执行：`curl -fsSL https://deno.land/x/install/install.sh | sh`
      1. 添加 deno 的环境变量。方便下次使用。 环境变量的添加，依赖你所使用终端环境。 比如：使用使用 zprofile 配置。 - a. `vim ~/.zprofile` - b. 添加 `export PATH=$PATH:/Users/bfchainer/.deno/bin` (deno 的安装路径，默认是: /Users/xxx/.deno/bin) - c. `source .zprofile`

1.  **安装 Node 与 包管理器**

    > 建议直接安装 node>=20

    - 1. `npm i -g pnpm`: 前端包管理器 pnpm，通常工具链的开发会用它
    - 1. `npm i -g yarn`: 前端包管理器 yarn，直接面向工程的开发会用它

1.  **安装 JDK17+**

1.  **建议安装**
    - [Visual Studio Code](https://code.visualstudio.com/download)
    - [Github Desktop](https://desktop.github.com/)

## 启动项目

1. 在项目目录下的 next/kmp 文件下存放着 Dweb Browser 基于 kotlin multiplatform project 的代码，使用 Android Studio 打开。
   1. C# 版本 (next/dweb-browser) 已经废弃
   1. 桌面开发版 (desktop-dev) 正在废弃中，我们正在开发正式的桌面版，所以原本的桌面开发版已经不再维护，也不再提供文档，目前只在内部作为过渡阶段临时使用。
1. 在项目根目录运行： `deno task dev`，它会自动拉取子仓库，并安装依赖，然后启动开发者模式
   - 所谓开发者模式，就是对一些资源文件做实时的自动化预编译，并实时复制到 iOS, Android，Desktop 等环境下。
   - 如果 deno 长时间运行，可能会出现一些文件监听失效或者子进程异常退出，那么重启再启动 `deno task dev` 即可
1. 编译，运行：
   - 如果是 Android 用户，直接启动模拟器或者 USB 连接开启调试等设备后，可以直接运行
   - 如果是 IOS 用户使用 Android Studio 启动项目，确保 `Kotlin Multiplatform Mobile` 插件已经安装，在`Edit Configurations`中，点击`+`，选`IOS Application`
     1. 在面板中，先选 `xcode project file`，路径在 `next/kmp/app/iosApp/iosApp.xcodeproj`
     1. 然后就可以选择 `scheme` 为 `iosApp` ； `configuration` 建议是 `Debug`
     1. 最后是 `target`，可以选模拟器，如果你的真机设备找不到，那么就卸载重装`Kotlin Multiplatform Mobile`插件，然后通常就能选择真机设备了，反正只要找不到，就重装插件就行
   - 如果是 IOS 用户也可以直接使用 xcode 打开`next/kmp/app/iosApp/iosApp.xcodeproj`，直接运行即可
   - 如果是 IOS 用户，注意因为 Kotlin 1.9.21 的增量编译还存在 BUG，修改 Compose 代码会存在异常（[KT-63789](https://youtrack.jetbrains.com/issue/KT-63789)），所以如果你在开发的过程中修改了 Compose 相关的代码，需要删除掉 shared/build 再进行编译。非 Compose 代码则基本不需要，但指不定还是会出问题，此时只能建议删除 shared/build 再做编译