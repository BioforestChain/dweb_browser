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
        let resultResolve: { (res: Response): void };
        const resultPromise = new Promise<Response>((resolve) => (resultResolve = resolve));
        match(event)
          .with({ pathname: "/getState" }, () => {
            const state = this.statusBarGetState();
            resultResolve(Response.json(state));
          })
          .with({ pathname: "/setState" }, () => {
            const states = parseQuery(event.searchParams, query_state);
            this.statusBarSetState(states);
            resultResolve(Response.json(null));
          })
          .with({ pathname: "/startObserve" }, () => {
            this.observer.startObserve(ipc);
            resultResolve(Response.json(true));
          })
          .with({ pathname: "/stopObserve" }, () => {
            this.observer.stopObserve(ipc);
            resultResolve(Response.json(""));
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

            resultResolve(
              new Response(readableStream, {
                status: 200,
                statusText: "ok",
                headers: new Headers({ "Content-Type": "application/octet-stream" }),
              })
            );
          });

        return await resultPromise;

        // // 获取状态栏状态
        // if (pathname.endsWith("/getState")) {
        //   const state = this.statusBarGetState();
        //   return Response.json(state);
        // }
        // if (pathname.endsWith("/setState")) {
        //   const states = parseQuery(searchParams, query_state);
        //   this.statusBarSetState(states);
        //   return Response.json(null);
        // }
        // // 开始订阅数据
        // if (pathname.endsWith("/startObserve")) {
        //   this.observer.startObserve(ipc);
        //   return Response.json(true);
        // }
        // // 结束订阅数据
        // if (pathname.endsWith("/stopObserve")) {
        //   this.observer.stopObserve(ipc);
        //   return Response.json("");
        // }

        // // 订阅
        // if (pathname.endsWith("/observe")) {
        //   const readableStream = new ReadableStream({
        //     start: (_controller) => {
        //       this.observer.observe(_controller);
        //     },
        //     pull(_controller) {},
        //     cancel: (reson) => {
        //       console.log("", "cancel", reson);
        //     },
        //   });

        //   return new Response(readableStream, {
        //     status: 200,
        //     statusText: "ok",
        //     headers: new Headers({ "Content-Type": "application/octet-stream" }),
        //   });
        // }
      })
      .forbidden()
      .cors();
    this.emitReady();
  })();

  observer = new StateObservable(() => {
    return JSON.stringify(this.statusBarGetState());
  });

  override emitUpdate(): void {
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
