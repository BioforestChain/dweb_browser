import { match } from "ts-pattern";
import { colorToHex, hexaToRGBA, parseQuery, z, zq } from "../../../deps.ts";
import { StateObservable } from "../helper/StateObservable.ts";
import { createMockModuleServerIpc } from "../helper/mokeServerIpcHelper.ts";
import { $BAR_STYLE, $BarState } from "../types.ts";
import { BaseController } from "./base-controller.ts";
export class StatusBarController extends BaseController {
  private _init = (async () => {
    this.emitInit();
    const ipc = await createMockModuleServerIpc("status-bar.nativeui.browser.dweb");
    const query_state = z.object({
      color: zq.transform((color) => colorToHex(JSON.parse(color))).optional(),
      style: z.enum(["DARK", "LIGHT", "DEFAULT"]).optional(),
      overlay: zq.boolean().optional(),
      visible: zq.boolean().optional(),
    });

    ipc
      .onFetch(async (event) => {
        return match(event)
          .with({ pathname: "/getState" }, () => {
            const state = this.statusBarGetState();
            return Response.json(state);
          })
          .with({ pathname: "/setState" }, () => {
            const states = parseQuery(event.searchParams, query_state);
            this.statusBarSetState(states);
            return Response.json(null);
          })
          .with({ pathname: "/startObserve" }, () => {
            this.observer.startObserve(ipc);
            return Response.json(true);
          })
          .with({ pathname: "/stopObserve" }, () => {
            this.observer.stopObserve(ipc);
            return Response.json("");
          })
          .run();
      })
      .forbidden()
      .cors();
    this.emitReady();
  })();

  observer = new StateObservable(() => {
    return JSON.stringify(this.statusBarGetState());
  });

  override emitUpdate(): void {
    console.log("status-bar.conroller.ts emitUpdate")
    this.observer.notifyObserver();
    super.emitUpdate();
  }

  state: $BarState = {
    color: "#FFFFFF80",
    style: "DEFAULT",
    insets: {
      top: 38,
      right: 0,
      bottom: 0,
      left: 0,
    },
    overlay: false,
    visible: true,
  };

  statusBarSetState(state: Partial<$BarState>) {
    this.state = {
      ...this.state,
      /// 这边这样做的目的是移除undefined值
      ...JSON.parse(JSON.stringify(state)),
    };
    this.emitUpdate();
  }

  statusBarSetStyle(style: $BAR_STYLE) {
    this.state = {
      ...this.state,
      style: style,
    };
    this.emitUpdate();
  }

  statusBarSetBackground(color: string) {
    this.state = {
      ...this.state,
      color: color,
    };
    this.emitUpdate();
  }

  statusBarSetOverlay(overlay: boolean) {
    this.state = {
      ...this.state,
      overlay: overlay,
    };
    this.emitUpdate();
  }

  statusBarSetVisible(visible: boolean) {
    this.state = {
      ...this.state,
      visible: visible,
    };
    this.emitUpdate();
  }

  statusBarGetState() {
    return {
      ...this.state,
      color: hexaToRGBA(this.state.color),
    };
  }
}
