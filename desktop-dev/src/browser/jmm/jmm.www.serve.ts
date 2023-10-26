import { $MMID } from "../../core/types.ts";
import { tryDevUrl } from "../../helper/electronIsDev.ts";
import { createNativeWindow } from "../../helper/openNativeWindow.ts";
import { buildUrl } from "../../helper/urlHelper.ts";
import { HttpDwebServer } from "../../std/http/helper/$createHttpDwebServer.ts";
import type { JmmNMM } from "./jmm.ts";

export class JmmServer {
  constructor(
    private mm: JmmNMM,
    private jmmUrl: string,
    private jmmServer: HttpDwebServer
  ) {}

  private win?: Electron.BrowserWindow = undefined;

  close() {
    this.win?.close();
  }

  async show(metadataUrl: string) {
    if(!this.win) {
      this.win = await createNativeWindow(this.mm.mmid, {
        width: 350,
        height: 750,
        show: false,
      });
      this.win.setVisibleOnAllWorkspaces(true);
      this.win.addListener("close", _ => {
        this.win = undefined;
      });
    }

    this.win.loadURL(
      buildUrl(this.jmmUrl, {
        search: {
          metadataUrl: metadataUrl,
        },
      }).href
    );
    this.win.webContents.openDevTools({ mode:"detach"})
    this.win.show();
  }

  static async create(mm: JmmNMM, jmmServer: HttpDwebServer) {
    const jmmProdUrl = jmmServer.startResult.urlInfo.buildInternalUrl((url) => {
      url.pathname = "/index.html";
    }).href;
    const jmmUrl = await tryDevUrl(jmmProdUrl, `http://localhost:3601/index.html`);

    // const jmmWin = await createNativeWindow(mm.mmid, {
    //   width: 350,
    //   height: 750,
    //   show: false,
    // });
    // jmmWin.setVisibleOnAllWorkspaces(true);
    return new JmmServer(mm, jmmUrl, jmmServer);
  }
}
