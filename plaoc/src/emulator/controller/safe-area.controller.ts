import { match } from "ts-pattern";
import { parseQuery, z, zq } from "../../../deps.ts";
import { StateObservable } from "../helper/StateObservable.ts";
import { getButtomBarState } from "../multi-webview-comp-safe-area.shim.ts";
import { createMockModuleServerIpc } from "./../helper/mokeServerIpcHelper.ts";
import { BaseController } from "./base-controller.ts";
import type { $Insets } from "../../client/util/insets.ts";

export class SafeAreaController extends BaseController {
  private _init = (async () => {
    this.emitInit();
    const ipc = await createMockModuleServerIpc("safe-area.nativeui.browser.dweb");
    const query_state = z.object({
      overlay: zq.boolean(),
    });
    ipc
      .onFetch(async (event) => {
        return match(event)
          .with({ pathname: "/getState" }, () => {
            return Response.json(this.state);
          })
          .with({ pathname: "/setState" }, () => {
            const states = parseQuery(event.searchParams, query_state);
            this.safeAreaSetOverlay(states.overlay);
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
    return JSON.stringify(this.state);
  });

  override emitUpdate(): void {
    this.observer.notifyObserver();
    super.emitUpdate();
  }

  state = {
    overlay: false,
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

  safgeAreaUpdateState = (
    statusBarState: {
      visible: boolean;
      overlay: boolean;
      insets: $Insets;
    },
    navigationBarState: {
      visible: boolean;
      overlay: boolean;
      insets: $Insets;
    },
    virtualKeyboardState: {
      overlay: boolean;
      visible: boolean;
      insets: $Insets;
    },
    isShowVirtualKeyboard: boolean
  ) => {
    const bottomBarState = getButtomBarState(navigationBarState, isShowVirtualKeyboard, virtualKeyboardState);
    this.state = {
      overlay: statusBarState.overlay && bottomBarState.overlay,
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
    // this.emitUpdate();
    // 只发送给监听 但是不触发 updated
    this.observer.notifyObserver();
    return this.state;
  };

  safeAreaSetOverlay = (overlay: boolean) => {
    this.state.overlay = overlay;
    this.emitUpdate();
  };
}

 