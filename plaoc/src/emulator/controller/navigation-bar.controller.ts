import { match } from "ts-pattern";
import { colorToHex, hexaToRGBA, parseQuery, z, zq } from "../../../deps.ts";
import { StateObservable } from "../helper/StateObservable.ts";
import { createMockModuleServerIpc } from "../helper/mokeServerIpcHelper.ts";
import { $BAR_STYLE, $BarState } from "../types.ts";
import { BaseController } from "./base-controller.ts";

export class NavigationBarController extends BaseController {
  private _init = (async () => {
    this.emitInit();
    const ipc = await createMockModuleServerIpc("navigation-bar.nativeui.browser.dweb");
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
            const state = this.navigationBarGetState();
            return Response.json(state);
          })
          .with({ pathname: "/setState" }, () => {
            const states = parseQuery(event.searchParams, query_state);
            this.navigationBarSetState(states);
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
          .with({ pathname: "/observe" }, () => {
            const readableStream = new ReadableStream({
              start: (_controller) => {
                this.observer.observe(_controller);
              },
              pull(_controller) {},
              cancel: (reson) => {
                console.log("", "cancel", reson);
              },
            });

            return new Response(readableStream, {
              status: 200,
              statusText: "ok",
              headers: new Headers({ "Content-Type": "application/octet-stream" }),
            });
          })
          .run();
      })
      .forbidden()
      .cors();
    this.emitReady();
  })();
  observer = new StateObservable(() => {
    return JSON.stringify(this.navigationBarGetState());
  });

  override emitUpdate(): void {
    this.observer.notifyObserver();
    super.emitUpdate();
  }

  state: $BarState = {
    color: "#FFFFFFFF",
    style: "DEFAULT",
    insets: {
      top: 0,
      right: 0,
      bottom: 26,
      left: 0,
    },
    overlay: false,
    visible: true,
  };

  navigationBarSetState(state: Partial<$BarState>) {
    this.state = {
      ...this.state,
      /// 这边这样做的目的是移除undefined值
      ...JSON.parse(JSON.stringify(state)),
    };
    this.emitUpdate();
  }

  navigationBarSetStyle(style: $BAR_STYLE) {
    this.state = {
      ...this.state,
      style: style,
    };
    this.emitUpdate();
  }

  navigationBarSetBackground(color: string) {
    this.state = {
      ...this.state,
      color: color,
    };
    this.emitUpdate();
  }

  navigationBarSetOverlay(overlay: boolean) {
    this.state = {
      ...this.state,
      overlay: overlay,
    };
    this.emitUpdate();
  }

  navigationBarSetVisible(visible: boolean) {
    this.state = {
      ...this.state,
      visible: visible,
    };
    this.emitUpdate();
  }

  navigationBarGetState() {
    return {
      ...this.state,
      color: hexaToRGBA(this.state.color),
    };
  }
}
