// api.sys.dweb模块
// 用来链接 第三方程序中注入的 webComponent 同 模拟系统功能的模块【类似 statusbar这样的】
// 数据流动方向
// 第三方应用 调用注入的 webCompoennt 暴露的方法
// -> common.worker.mts 模块拦截 webCompoennt 方法发出的请求
// -> common.worker.mts 模块把请求发送给 昂前 api.sys.dweb 模块
// -> api.sys.dweb 模块把请求发送刚给对应的 模拟系统功能的模块【类似 statusbar这样的】
// 从而实现对 系统功能的调用
import chalk from "chalk";
import { NativeMicroModule } from "../../core/micro-module.native.cjs";

export class ApiNMM extends NativeMicroModule {
  mmid = "api.sys.dweb" as const;
  async _bootstrap() {
    console.log(chalk.red("[api.cts 启动了]"));
    // 注册通过 jsProcess 发送过来的访问请求
    // 专门用来做静态服务
    this.registerCommonIpcOnMessageHandler({
      method: "PUT",
      pathname: "/statusbar",
      matchMode: "full",
      input: {},
      output: {},
      handler: async (args, client_ipc, request) => {
        const _url = `file://statusbar.sys.dweb/operation_from_plugins?app_url=${request.parsed_url.searchParams.get(
          "app_url"
        )}`;
        return this.fetch(_url, {
          method: request.method,
          body: request.body.raw,
          headers: request.headers,
        });
      },
    });
  }

  protected _shutdown(): unknown {
    throw new Error("Method not implemented.");
  }
}
