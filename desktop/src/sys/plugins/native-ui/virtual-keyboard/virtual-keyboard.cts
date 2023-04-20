import { NativeMicroModule } from "../../../../core/micro-module.native.cjs";
import type { Remote } from "comlink";
import type { Ipc } from "../../../../core/ipc/ipc.cjs";
import type { $NativeWindow } from "../../../../helper/openNativeWindow.cjs";
import { log } from "../../../../helper/devtools.cjs"
import { PluginsRequest } from "../../plugins-request.cjs"
import { WWWServer } from "./www-server.cjs"
import { AddRoutesToHttp } from "./add-routes-to-http.cjs"

// @ts-ignore
type $APIS = typeof import("./assets/multi-webview.html.mjs")["APIS"];
export class VirtualKeyboardNMM extends NativeMicroModule {
  mmid = "virtual-keyboard.nativeui.sys.dweb" as const;
  httpIpc: Ipc | undefined
  pluginsRequest = new PluginsRequest();
  private _uid_wapis_map = new Map<
    number,
    { nww: $NativeWindow; apis: Remote<$APIS> }
  >();

  _bootstrap = async (context: any) => {
    log.green(`[${this.mmid} _bootstrap]`)
    {
      new WWWServer(this)
    }

    {
      new AddRoutesToHttp(this, context)
    }
  }

  _shutdown = async () => {

  }
}
