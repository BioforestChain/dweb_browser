## 环境安装

> 记得科学上网，否则部分软件下载的速度会非常慢。

1.  **Xcode 安装**：
    AppStroe 下载

2.  **安装 Android Studio**

    - 下载安装包，->[地址](https://developer.android.com/studio/preview),
      Apple 芯片的记得选用： Mac with Apple chip 版本。
    - 安装安装引导，一路 next + agree。
    - License Agreement 记得所有的 license 都点 agree, 否则，无法 next.

3.  **安装 Visual Studio for Mac**

    - 下载安装包，->[地址](https://visualstudio.microsoft.com/zh-hans/thank-you-downloading-visual-studio-mac/?sku=communitymac&rel=17)
    - 安装引导，一路 next + agree。
    - 安装插件：
      - [Compose Multiplatform IDE Support](https://plugins.jetbrains.com/plugin/16541-compose-multiplatform-ide-support)
      - [Kotlin Multiplatform Mobile](https://plugins.jetbrains.com/plugin/index?xmlId=com.jetbrains.kmm)

4.  **安装 Deno**

    - 终端记得翻墙
    - 终端上执行：`curl -fsSL https://deno.land/x/install/install.sh | sh`
    - 添加 deno 的环境变量。方便下次使用。 环境变量的添加，依赖你所使用终端环境。 比如：使用使用 zprofile 配置。 - a. `vim ~/.zprofile` - b. 添加 `export PATH=$PATH:/Users/bfchainer/.deno/bin` (deno 的安装路径，默认是: /Users/xxx/.deno/bin) - c. `source .zprofile`

5.  **安装 Node**

    - 终端先提前安装好 HomeBrew(已安装，直接下一步 2):
      - 1. `/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install.sh)”`
    - 安装 Node:
      - 1. `brew install node@18`
      - 1. `npm i -g pnpm`: 前端包管理器 pnpm，通常工具链的开发会用它
      - 1. `npm i -g yarn`: 前端包管理器 yarn，直接面向工程的开发会用它

6.  **建议安装**
    - [Visual Studio Code](https://code.visualstudio.com/download)
    - [Github Desktop](https://desktop.github.com/)

## 启动项目

1. 在项目目录下的 next/kmp 文件下存放着 DWebBrowser 的多源代码，使用 AndroidStudio 打开。
1. 使用 VS 打开项目工程文件：/项目路径/dweb_browser/next/dweb-browser.sln。
1. 在项目根目录运行： `deno task dev`，它会自动拉取子仓库，并安装依赖，然后启动开发者模式，对一些资源文件做自动化预编译，并实时复制到 iOS, Android，Desktop 等环境下。
1. 编译，运行：
   - 如果是 Android 用户，直接启动模拟器或者 USB 连接开启调试等设备后，可以直接运行
   - 如果是 IOS 用户，确保 `Kotlin Multiplatform Mobile` 插件已经安装，在`Edit Configurations`中，点击`+`，选`IOS Application`
     1. 在面板中，先选 `xcode project file`，路径在 `next/kmp/app/iosApp/iosApp.xcodeproj`
     1. 然后就可以选择 `scheme` 为 `iosApp` ； `configuration` 建议是 `Debug`
     1. 最后是 `target`，可以选模拟器，如果你的真机设备找不到，那么就卸载重装`Kotlin Multiplatform Mobile`插件，然后通常就能选择真机设备了，反正只要找不到，就重装插件就行
     1. 当然，也可以直接用 xcode 打开`next/kmp/app/iosApp/iosApp.xcodeproj`，直接运行即可
