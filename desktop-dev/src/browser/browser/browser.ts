import { $BootstrapContext } from "../../core/bootstrapContext.ts";
import { NativeMicroModule } from "../../core/micro-module.native.ts";
import { HttpDwebServer } from "../../std/http/helper/$createHttpDwebServer.ts";
import { createBrowserWindow } from "./browser.bw.ts";
import { createWWWServer } from "./browser.serve.www.ts";
import { canGoBack, canGoForward, goBack, goForward, refresh, updateContent } from "./browser.server.api.ts";

import type { $CBV } from "./browser.content.bv.ts";

export class BrowserNMM extends NativeMicroModule {
  mmid = "browser.dweb" as const;
  wwwServer: HttpDwebServer | undefined;
  bw: Awaited<ReturnType<typeof createBrowserWindow>> | undefined;
  contentBV: $CBV | undefined;
  addressBV: Electron.BrowserView | undefined;
  // addressBVHeight = 38
  addressBVHeight = 0; // 没有地址栏

  protected async _bootstrap(context: $BootstrapContext) {
    this.wwwServer = await createWWWServer.call(this);
    this.bw = await createBrowserWindow.call(
      this,
      this.wwwServer.startResult.urlInfo.buildInternalUrl((url) => {
        url.pathname = "/index.html";
      }).href
    );
    // this.contentBV = createCBV.bind(this)(this.bw, addressBarHeight);

    this.registerCommonIpcOnMessageHandler({
      pathname: "/update/content",
      matchMode: "full",
      input: {},
      output: "object",
      handler: updateContent.bind(this),
    });
    this.registerCommonIpcOnMessageHandler({
      pathname: "/can-go-back",
      matchMode: "full",
      input: {},
      output: "object",
      handler: canGoBack.bind(this),
    });
    this.registerCommonIpcOnMessageHandler({
      pathname: "/can-go-forward",
      matchMode: "full",
      input: {},
      output: "object",
      handler: canGoForward.bind(this),
    });
    this.registerCommonIpcOnMessageHandler({
      pathname: "/go-back",
      matchMode: "full",
      input: {},
      output: "object",
      handler: goBack.bind(this),
    });
    this.registerCommonIpcOnMessageHandler({
      pathname: "/go-forward",
      matchMode: "full",
      input: {},
      output: "object",
      handler: goForward.bind(this),
    });
    this.registerCommonIpcOnMessageHandler({
      pathname: "/refresh",
      matchMode: "full",
      input: {},
      output: "object",
      handler: refresh.bind(this),
    });
  }

  private _addressBarHeight() {
    // argv 有 --inspect === 调试状态 增加 addressBar 的高度;
    // 非调试状态选用标准高度
    // return process.argv.includes("--inspect") ? 138 : this.addressBVHeight;
    return this.addressBVHeight;
  }
  protected _shutdown() {}
}
