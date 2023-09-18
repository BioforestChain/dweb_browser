**iOS环境安装**
> **前言：**
> 	1.记得先翻墙，否则部分软件下载的速度会非常慢。
> 	2.Apple芯片的电脑记得选用： Mac with Apple chip版本

- 1.**Xcode 安装**：
	AppStroe下载

- 2.**安装Android Studio**
	- 下载安装包，->[地址](https://developer.android.com/studio/preview),
	Apple芯片的记得选用： Mac with Apple chip版本。
	- 安装安装引导，一路next + agree。 
	- License Agreement记得所有的license都点agree, 否则，无法next.


- 3.**安装Visual Studio for Mac**
	- 下载安装包，->[地址](https://visualstudio.microsoft.com/zh-hans/thank-you-downloading-visual-studio-mac/?sku=communitymac&rel=17)
	- 安装引导，一路next + agree。 

- 4.**安装Deno**
	- 终端记得翻墙
	- 终端上执行：`curl -fsSL https://deno.land/x/install/install.sh | sh`
	- 添加deno的环境变量。方便下次使用。 环境变量的添加，依赖你所使用终端环境。 比如：使用使用zprofile配置。
				- a. `vim ~/.zprofile`
				- b. 添加 `export PATH=$PATH:/Users/bfchainer/.deno/bin` (deno的安装路径，默认是: /Users/xxx/.deno/bin)
				- c. `source .zprofile`
5. **安装Node**
	- 终端先提前安装好HomeBrew(已安装，直接下一步2):
		- a. `/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install.sh)”`
	- 安装Node:
		- a. `brew install node@18`
		- b. `npm i -g pnpm`: 前端包管理器

6.**安装Visual Studio Code**
	- 下载地址：https://code.visualstudio.com/download

7.**运行DWebBrowser多源代码**
1. 拉取仓库：https://github.com/BioforestChain/dweb_browser.git。相关权限可以让肇丰帮忙配置。
2. 运行iOS版本: 在项目目录下的ios文件夹存放着纯iOS的代码，可直接编译运行。
3. 运行多源版本：
* 在项目目录下的next文件下存放着DWebBrowser的多源代码。
* 由于需要将iOS的DWebBrowser打包成frameWork,因此需要配置Apple开发者账号。请向相关人员索要或者使用其他人的账号。
* 运行在项目的iOS目录下的打包framework的脚本的build.sh。
* 使用VS打开项目工程文件：/项目路径/dweb_browser/next/dweb-browser.sln。
* 在项目根目录运行：
* 1. `deno task init`: 安装相关依赖。 
+ 2. `deno task dev`: 启动开发者模式，前端项目会自动编译并复制到iOS, 安卓，桌面端的项目。
* .选择模拟器编译，运行。

	
