//  jmm.sys.dweb 负责启动 第三方应用程序

import type { $BootstrapContext } from "../../core/bootstrapContext.cjs";
import { IpcResponse } from "../../core/ipc/IpcResponse.cjs";
import { NativeMicroModule } from "../../core/micro-module.native.cjs";
import { $JmmMetadata, JmmMetadata } from "./JmmMetadata.cjs";
import { JsMicroModule } from "./micro-module.js.cjs";

// 运行在 node 环境
export class JmmNMM extends NativeMicroModule {
  mmid = "jmm.sys.dweb" as const;
  readonly apps = new Map<$MMID, JsMicroModule>();

  async _bootstrap(context: $BootstrapContext) {
    for (const app of this.apps.values()) {
      context.dns.install(app);
    }
    //  安装 第三方 app
    this.registerCommonIpcOnMessageHandler({
      pathname: "/install",
      matchMode: "full",
      input: { metadataUrl: "string" },
      output: "boolean",
      handler: async (args, client_ipc, request) => {
        await this.openInstallPage(args.metadataUrl);
        return true;
      },
    });

    // 专门用来做静态服务
    this.registerCommonIpcOnMessageHandler({
      pathname: "/open",
      matchMode: "full",
      input: {},
      output: "boolean",
      handler: async (args, client_ipc, request) => {
        const _url = new URL(request.url);
        let appId = _url.searchParams.get("appId");
        if (appId === null) return false;
        // 注意全部需要小写
        const mmid = createMMIDFromAppID(appId);
        const response = await this.nativeFetch(
          `file://dns.sys.dweb/open?app_id=${mmid}`
        );
        return IpcResponse.fromResponse(request.req_id, response, client_ipc);
      },
    });
  }

  protected _shutdown(): unknown {
    throw new Error("Method not implemented.");
  }

  private async openInstallPage(metadataUrl: string) {
    const config = await this.nativeFetch(metadataUrl).object<$JmmMetadata>();
    // TODO 打开安装页面

    /// 成功安装
    new JsMicroModule(new JmmMetadata(config));
    return config;
  }
}

/**
 * 创建 mmid 根据appId
 */
function createMMIDFromAppID(appId: string) {
  return `app.${appId.toLocaleLowerCase()}.dweb` as $MMID;
}
