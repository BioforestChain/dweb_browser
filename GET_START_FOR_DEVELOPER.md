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
    - 1. `npm i -g yarn`: 前端包管理器 yarn，直接面向工程的开发会用它，Android Studio 运行 jsTarget 的时候也会用到它
         > 确保版本大于等于 1.22.21

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
     1. 在面板中，先选 `xcode project file`，路径在 `next/kmp/app/iosApp/DwebBrowser.xcworkspace`
     1. 然后就可以选择 `scheme` 为 `DwebBrowser` ； `configuration` 建议是 `Debug`
     1. 最后是 `target`，可以选模拟器，如果你的真机设备找不到，那么就卸载重装`Kotlin Multiplatform Mobile`插件，然后通常就能选择真机设备了，反正只要找不到，就重装插件就行
   - 如果是 IOS 用户也可以直接使用 xcode 打开`next/kmp/app/iosApp/DwebBrowser.xcworkspace`，直接运行 DwebBrowser 即可

## 小提示 Tips

1. 使用 Android Studio 开发 jsTarget 的时候，如果遇到`yarn.lock was changed. Run the 'kotlinUpgradeYarnLock' task to actualize yarn.lock file` 这样的错误，那么在 Android Studio 的 Gradle 面板中选择第一个按钮“Execute Gradle Task”，在弹出面板的 input 中，填写`gradle kotlinUpgradeYarnLock`，运行即可。
1. 便携 gradle 的脚本需要经常需要处理依赖关系，如果你不知道有哪些 task 可以被你调用，可以参考这些小知识点：
   1. 想要获得 gradlew 的所有 tasks，到 kmp/next 目录下，执行: `./gradlew -q :tasks --all > .gradle/all.md`
   1. 想要获得 gradlew 的指定 project 的 tasks，到 kmp/next 目录下，执行: `./gradlew -q helper:tasks --all > .gradle/helper.md`
1. 不要使用 Fleet 打开项目，因为它会往 DwebBrowser.xcworkspace 文件夹里头加入一些文件导致 xcodebuild 脚本失效，如果你不幸使用 Fleet 打开过项目，请手动删除 DwebBrowser.xcworkspace 文件夹里头`[Fleet]`打头的文件
1. 使用 [composeResources](https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-images-resources.html#resource-usage) 文件夹存储资源文件时，需要运行 ./gradlaw generateComposeResClass 来自动生成代码
