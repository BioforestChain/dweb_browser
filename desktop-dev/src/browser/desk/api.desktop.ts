import { $BootstrapContext } from "../../core/bootstrapContext.ts";
import { MICRO_MODULE_CATEGORY } from "../../core/category.const.ts";
import { HttpDwebServer } from "../../std/http/helper/$createHttpDwebServer.ts";
import { DeskNMM } from "./desk.nmm.ts";
import { deskStore } from "./desk.store.ts";

export class DesktopApi {
  constructor(
    private win: Electron.BrowserWindow,
    private mm: DeskNMM,
    private context: $BootstrapContext,
    private taskbarServer: HttpDwebServer,
    private desktopServer: HttpDwebServer
  ) {}
  private _appOrders = deskStore.get("desktop/orders", () => new Map());
  /** 列出桌面的应用列表 */
  async getDesktopAppList() {
    const apps = await this.context.dns.search(MICRO_MODULE_CATEGORY.Application);
    return apps
      .map((metaData) => {
        return { ...metaData, running: this.mm.runingApps.has(metaData.mmid) };
      })
      .sort((a, b) => {
        const aOrder = this._appOrders.get(a.mmid)?.order ?? 0;
        const bOrder = this._appOrders.get(b.mmid)?.order ?? 0;
        return aOrder - bOrder;
      });
  }
  close() {
    return this.win.close();
  }
}
