import "ts-pattern";
import { match } from "ts-pattern";
import { IPC_METHOD } from "../../client/components/dweb-service-worker/dweb-service-worker.type.ts";
import { createMockModuleServerIpc } from "../helper/mokeServerIpcHelper.ts";
import type { $ShareOptions } from "../types.ts";
import { BaseController } from "./base-controller.ts";
 
export class ShareController extends BaseController {
  constructor(readonly share: $Share) {
    super();
  }
  private _init = (async () => {
    this.emitInit();
    const ipc = await createMockModuleServerIpc("share.sys.dweb");
    ipc
      .onFetch(async (event) => {
        return match(event)
          .with({ method: IPC_METHOD.POST as any, pathname: "/share" }, async () => {
            const title = event.searchParams.get('title');
            const text = event.searchParams.get("text");
            const url = event.searchParams.get('url');
            const body = await event.arrayBuffer()
            const bodyType = event.headers.get("Content-Type")
            const options = {
              title: title ? title : "",
              text: text ? text : "",
              link: url ? url: "",
              src: "",
              body: new Uint8Array(body),
              bodyType: bodyType ? bodyType : ""
            }
            this.share(options)
            return Response.json(true);
          })
          .run();
      })
      .forbidden()
      .cors();
    this.emitReady();
  })();

  override emitUpdate(): void {
    super.emitUpdate();
  }
}

export interface $Share {
  (options: $ShareOptions): void;
}


// cd ../plaoc && deno task build:demo &&  deno task build:server && cd ../desktop-dev && deno task dnt --start install --url http://127.0.0.1:8096/metadata.json


