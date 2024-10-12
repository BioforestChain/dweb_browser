# UI Test Flow
> 当前仅支持在 MacOS 平台上面进行UI自动化测试，且iOS使用模拟器进行测试
1. [Install Maestro](https://maestro.mobile.dev/getting-started/installing-maestro)
2. Run UITest `deno task uitest`

## Q&A
### 为什么运行了`deno task uitest`后安卓真机没有启动Ui测试，报了`Android driver unreachable`错误？
到安卓设备的开发者设置中找到  `USB 调试（安全设置）` ，如果有这个选项的话，开启就可以了。
