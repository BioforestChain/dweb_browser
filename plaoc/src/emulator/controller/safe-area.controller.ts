import { match } from "ts-pattern";
import { parseQuery, z, zq } from "../../../deps.ts";
import { StateObservable } from "../helper/StateObservable.ts";
import { getButtomBarState } from "../multi-webview-comp-safe-area.shim.ts";
import { $BarState } from "../types.ts";
import { createMockModuleServerIpc } from "./../helper/mokeServerIpcHelper.ts";
import { BaseController } from "./base-controller.ts";
export class SafeAreaController extends BaseController {
  private _init = (async () => {
    this.emitInit();
    const ipc = await createMockModuleServerIpc("safe-area.nativeui.browser.dweb");
    const query_state = z.object({
      overlay: zq.boolean(),
    });
    ipc
      .onFetch(async (event) => {
        let resultResolve: { (res: Response): void };
        const resultPromise = new Promise<Response>((resolve) => (resultResolve = resolve));
        match(event)
          .with({ pathname: "/getState" }, () => {
            resultResolve(Response.json(this.state));
          })
          .with({ pathname: "/setState" }, () => {
            const states = parseQuery(event.searchParams, query_state);
            this.safeAreaSetOverlay(states.overlay);
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
        // const { pathname, searchParams } = event;
        // // 获取安全区域状态
        // if (pathname.endsWith("/getState")) {
        //   return Response.json(this.state);
        // }
        // if (pathname.endsWith("/setState")) {
        //   const states = parseQuery(searchParams, query_state);
        //   this.safeAreaSetOverlay(states.overlay);
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
    return JSON.stringify(this.state);
  });

  override emitUpdate(): void {
    this.observer.notifyObserver();
    super.emitUpdate();
  }

  state = {
    overlay: false,
  };

  safeAreaGetState = () => {
    return {
      overlay: this.state.overlay,
      insets: {
        left: 0,
        top: 0,
        right: 0,
        bottom: 0,
      },
      cutoutInsets: {
        left: 0,
        top: 0,
        right: 0,
        bottom: 0,
      },
      // 外部尺寸
      outerInsets: {
        left: 0,
        top: 0,
        right: 0,
        bottom: 0,
      },
    };
  };

  safgeAreaUpdateState = (
    statusBarState: $BarState,
    navigationBarState: $BarState,
    virtualKeyboardState: $BarState,
    isShowVirtualKeyboard: boolean
  ) => {
    const bottomBarState = getButtomBarState(navigationBarState, isShowVirtualKeyboard, virtualKeyboardState);
    this.emitUpdate();
    return {
      overlay: this.state,
      insets: {
        left: 0,
        top: statusBarState.overlay ? statusBarState.insets.top : 0,
        right: 0,
        bottom: bottomBarState.overlay ? bottomBarState.insets.bottom : 0,
      },
      cutoutInsets: {
        left: 0,
        top: statusBarState.insets.top,
        right: 0,
        bottom: 0,
      },
      // 外部尺寸
      outerInsets: {
        left: 0,
        top: statusBarState.overlay ? 0 : statusBarState.insets.top,
        right: 0,
        bottom: bottomBarState.overlay ? 0 : bottomBarState.insets.bottom,
      },
    };
  };

  safeAreaSetOverlay = (overlay: boolean) => {
    this.state.overlay = overlay;
    this.emitUpdate();
  };
}
