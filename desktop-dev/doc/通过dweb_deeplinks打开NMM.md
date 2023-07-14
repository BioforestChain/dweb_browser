# 文档概述

- 用来描述 如何通过 dweb_deeplink 实现通过命令行打开指定的 NMM；

## NMM 中的准备工作

```ts
// 注意
// dweb_depplinks.pathname === registerCommonIpcOnMessageHandler.pathname
export class NameNMM extends NativeMicroModule {
  override dweb_deeplinks = ["dweb:pathname"] as $DWEB_DEEPLINK[];

  _bootstrap = async () => {
    /// 接受 dweb_deeplink 指令
    this.registerCommonIpcOnMessageHandler({
      protocol: "dweb:",
      pathname: "pathname",
      matchMode: "full",
      input: {},
      output: "void",
      handler: async (args) => {
        // 执行某项操作
      },
    });
  };
}
```

## 启动指令

<!-- pathname === dweb_deeplink.pathname -->

- `deno task dnt --start pathname --paramKey paramvalue --paramkey2 value2`
