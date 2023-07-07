import { z, zq } from "../../../deps.ts";
import { createMockModuleServerIpc } from "../helper/mokeServerIpcHelper.ts";
import { BaseController } from "./base-controller.ts";

export class HapticsController extends BaseController {
  private _init = (async () => {
    this.emitInit();
    const ipc = await createMockModuleServerIpc("haptics.sys.dweb");
    const query_state = z.object({
      type: zq.string().optional(),
      duration: zq.number().optional(),
    });
    ipc
      .onFetch((event) => {
        const { pathname, searchParams } = event;
        const state = zq.parseQuery(searchParams, query_state);
        this.hapticsMock(JSON.stringify({ pathname, state }));
        return Response.json(true);
      })
      .forbidden()
      .cors();
    this.emitReady();
  })();

  hapticsMock(text: string) {
    console.log("hapticsMock", text);
    this.emitUpdate();
  }
}
