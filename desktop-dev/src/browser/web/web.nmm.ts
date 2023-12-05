import { $BootstrapContext } from "../../core/bootstrapContext.ts";
import { MICRO_MODULE_CATEGORY } from "../../core/category.const.ts";
import { NativeMicroModule } from "../../core/micro-module.native.ts";
import { $DWEB_DEEPLINK } from "../../core/types.ts";
import { $Callback, createSignal } from "../../helper/createSignal.ts";
import { tryDevUrl } from "../../helper/electronIsDev.ts";
import { createNativeWindow } from "../../helper/openNativeWindow.ts";
import { fetchMatch } from "../../helper/patternHelper.ts";
import { buildUrl } from "../../helper/urlHelper.ts";
import { z, zq } from "../../helper/zodHelper.ts";
import { createWWWServer } from "./server.www.ts";

export class WebBrowserNMM extends NativeMicroModule {
  mmid = "web.browser.dweb" as const;
  name = "Web Browser";
  override short_name = "Browser";
  override dweb_deeplinks = ["dweb://search", "dweb://openinbrowser"] as $DWEB_DEEPLINK[];
  override categories = [MICRO_MODULE_CATEGORY.Application, MICRO_MODULE_CATEGORY.Web_Browser];
  override icons: NativeMicroModule["icons"] = [
    {
      src: "file:///sys/browser/web/logo.svg",
    },
  ];

  private win?: Electron.BrowserWindow;
  protected async _bootstrap(context: $BootstrapContext) {
    const wwwServer = await createWWWServer.call(this);
    const host = wwwServer.startResult.urlInfo.buildInternalUrl((url) => {
      url.pathname = "/desktop.html";
    }).href;
    const browserUrl = await tryDevUrl(host, "http://localhost:3500");
    // 激活的逻辑
    this.onRenderer(async (event, ipc) => {
      this.win = await this.openBrowserWindow(browserUrl);
      this.win?.addListener("close", (_) => {
        this.win = undefined;
      });
      void this.win?.loadURL(
        buildUrl(browserUrl, {
          search: {
            "api-base": host,
            mmid: this.mmid,
          },
        }).href
      );
    });
    //todo observer/app  openinbrowser

    const query_q = zq.object({ q: z.string().url() });
    const query_url = zq.object({ url: z.string().url() });
    const onFetchMatcher = fetchMatch()
      .deeplink("search", async (event) => {
        const { q: url } = query_q(event.searchParams);
        if(!this.win) {
          this.win = await this.openBrowserWindow(url);
        }

        void this.win?.loadURL(url);
        return Response.json({ ok: true });
      })
      .deeplink("openinbrowser", async (event) => {
        const { url } = query_url(event.searchParams);
        if(!this.win) {
          this.win = await this.openBrowserWindow(url);
        }
        
        void this.win?.loadURL(url);
        return Response.json({ ok: true });
      });
    this.onFetch(onFetchMatcher.run).internalServerError();
  }

  private async openBrowserWindow(_url?: string): Promise<Electron.BrowserWindow | undefined> {
    if (this.win) {
      this.win.close();
      this.win = undefined;
      return;
    }
    
    // 已经被打开过了
    this.win = await createNativeWindow(_url ?? "", {
      defaultBounds: { width: 1024, height: 700 },
    });

    this._onShutdown(() => {
      this.win?.close();
    });

    return this.win;
  }

  private _shutdown_signal = createSignal<$Callback>();
  private _onShutdown = this._shutdown_signal.listen;
  protected _shutdown() {
    this._shutdown_signal.emitAndClear();
  }
}
