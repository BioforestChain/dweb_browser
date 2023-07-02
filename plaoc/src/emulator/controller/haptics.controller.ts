import { z, zq } from "../../../deps.ts";
import { createMockModuleServerIpc } from "../helper/helper.ts";
import type { MultiWebViewCompMobileShell } from "../multi-webview-comp-mobile-shell.html.ts";

export class HapticsController {
  constructor(
    readonly multiWebviewCompMobileShell: MultiWebViewCompMobileShell | null
  ) {
    void this._init();
  }
  private async _init() {
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
      .cros()
      .forbidden();
  }

  hapticsMock(text: string) {
    console.log("hapticsMock", text);
  }
}
