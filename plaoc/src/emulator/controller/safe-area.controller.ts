import { parseQuery, z, zq } from "../../../deps.ts";
import { StateObservable } from "../helper/StateObservable.ts";
import { getButtomBarState } from "../multi-webview-comp-safe-area.shim.ts";
import { $BarState } from "../types.ts";
import { createMockModuleServerIpc } from "./../helper/helper.ts";
import { BaseController } from "./base-controller.ts";
export class SafeAreaController extends BaseController {
  private _init = (async () => {
    const ipc = await createMockModuleServerIpc(
      "safe-area.nativeui.browser.dweb"
    );
    const query_state = z.object({
      overlay: zq.boolean(),
    });
    ipc
      .onFetch((event) => {
        const { pathname, searchParams } = event;
        // 获取安全区域状态
        if (pathname.endsWith("/getState")) {
          return Response.json(this.state);
        }
        if (pathname.endsWith("/setState")) {
          const states = parseQuery(searchParams, query_state);
          this.safeAreaSetOverlay(states.overlay);
          return Response.json(null);
        }
        // 开始订阅数据
        if (pathname.endsWith("/startObserve")) {
          this.observer.startObserve(ipc);
          return Response.json(true);
        }
        // 结束订阅数据
        if (pathname.endsWith("/stopObserve")) {
          this.observer.startObserve(ipc);
          return Response.json("");
        }
      })
      .forbidden()
      .cors();
  })();

  observer = new StateObservable(() => {
    return JSON.stringify(this.state);
  });

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
    const bottomBarState = getButtomBarState(
      navigationBarState,
      isShowVirtualKeyboard,
      virtualKeyboardState
    );
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
