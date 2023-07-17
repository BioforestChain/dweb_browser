import { match } from "ts-pattern";
import { parseQuery, z, zq } from "../../../deps.ts";
import { StateObservable } from "../helper/StateObservable.ts";
import { getButtomBarState } from "../multi-webview-comp-safe-area.shim.ts";
import { createMockModuleServerIpc } from "./../helper/mokeServerIpcHelper.ts";
import { BaseController } from "./base-controller.ts";
import type { NavigationBarController } from "./navigation-bar.controller.ts";
import type { StatusBarController } from "./status-bar.controller.ts";
import type { VirtualKeyboardController } from "./virtual-keyboard.controller.ts";
// import { statusBarPlugin } from "../../client/components/status-bar/status-bar.plugin.ts";
import type { $AgbaColor } from "../../client/util/color.ts";
import type { $Insets } from "../../client/util/insets.ts";
import type { $BAR_STYLE } from "../types.ts";

export class SafeAreaController extends BaseController {
  statusbarReadableDefaultController: ReadableStreamDefaultController | undefined;
  navigationReadableDefaultController: ReadableStreamDefaultController | undefined;
  vitualKeyboardReadableDefaultController: ReadableStreamDefaultController | undefined;
  statusbarState: {
    color: $AgbaColor;
    style: $BAR_STYLE;
    visible: boolean;
    overlay: boolean;
    insets: $Insets;
  };
  navigationbarState: {
    color: $AgbaColor;
    style: $BAR_STYLE;
    visible: boolean;
    overlay: boolean;
    insets: $Insets;
  };
  vitualKeyboardState: {
    overlay: boolean;
    visible: boolean;
    insets: $Insets;
  };
  textDecoder = new TextDecoder();

  constructor(
    readonly statusbarController: StatusBarController,
    readonly navigationController: NavigationBarController,
    readonly vitualKeyboardController: VirtualKeyboardController
  ) {
    super();
    this.statusbarState = this.statusbarController.statusBarGetState();
    this.navigationbarState = this.navigationController.navigationBarGetState();
    this.vitualKeyboardState = this.vitualKeyboardController.state;
  }

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
            this.statusbarController.statusBarSetState(states);
            this.navigationController.navigationBarSetState(states);
            this.vitualKeyboardController.virtualKeyboardSetOverlay(states.overlay);
            // 不直接设置 overlay 通过通过 监听其他的controller 实现设置
            // this.safeAreaSetOverlay(states.overlay);
            return Response.json(null);
          })
          .with({ pathname: "/startObserve" }, () => {
            this.observer.startObserve(ipc);
            this.statusbarControllerObserve();
            this.navigationContollerObserve();
            this.vitualKeyboardControllerObserve();
            this.statusbarController.observer.startObserve(ipc);
            this.navigationController.observer.startObserve(ipc);
            this.vitualKeyboardController.observer.startObserve(ipc);
            return Response.json(true);
          })
          .with({ pathname: "/stopObserve" }, () => {
            this.statusbarController.observer.stopObserve(ipc);
            this.navigationController.observer.stopObserve(ipc);
            this.vitualKeyboardController.observer.stopObserve(ipc);
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

  // safeAreaGetState = () => {
  //   return {
  //     overlay: this.state.overlay,
  //     insets: {
  //       left: 0,
  //       top: 0,
  //       right: 0,
  //       bottom: 0,
  //     },
  //     cutoutInsets: {
  //       left: 0,
  //       top: 0,
  //       right: 0,
  //       bottom: 0,
  //     },
  //     // 外部尺寸
  //     outerInsets: {
  //       left: 0,
  //       top: 0,
  //       right: 0,
  //       bottom: 0,
  //     },
  //   };
  // };

  /**
   * 监听 statusbar
   */
  statusbarControllerObserve = async () => {
    const readableStream = new ReadableStream({
      start: (_controller) => {
        this.statusbarController.observer.observe(_controller);
        this.statusbarReadableDefaultController = _controller;
      },
      pull(_controller) {},
      cancel: (reson) => {
        console.log("", "cancel", reson);
      },
    });

    let loop = true;
    const reader = readableStream.getReader();
    while (loop) {
      const { value, done } = await reader.read();
      if (value) {
        this.statusbarState = this.parseObserveValue(value);
        this.setStateByOtherControllerState();
      }
      loop = !done;
    }
  };

  navigationContollerObserve = async () => {
    const readableStream = new ReadableStream({
      start: (_controller) => {
        this.navigationController.observer.observe(_controller);
        this.navigationReadableDefaultController = _controller;
      },
      pull(_controller) {},
      cancel: (reson) => {
        console.log("", "cancel", reson);
      },
    });

    let loop = true;
    const reader = readableStream.getReader();
    while (loop) {
      const { value, done } = await reader.read();
      if (value) {
        this.navigationbarState = this.parseObserveValue(value);
        this.setStateByOtherControllerState();
      }
      loop = !done;
    }
  };

  vitualKeyboardControllerObserve = async () => {
    const readableStream = new ReadableStream({
      start: (_controller) => {
        this.vitualKeyboardController.observer.observe(_controller);
        this.vitualKeyboardReadableDefaultController = _controller;
      },
      pull(_controller) {},
      cancel: (reson) => {
        console.log("", "cancel", reson);
      },
    });

    let loop = true;
    const reader = readableStream.getReader();
    while (loop) {
      const { value, done } = await reader.read();
      if (value) {
        this.vitualKeyboardState = this.parseObserveValue(value);
        this.setStateByOtherControllerState();
      }
      loop = !done;
    }
  };

  parseObserveValue = (value: Uint8Array) => {
    return JSON.parse(this.textDecoder.decode(value));
  };

  /**
   * 根据其他的 controller 设置 状态
   */
  setStateByOtherControllerState = () => {
    this.safgeAreaUpdateState(
      this.statusbarState,
      this.navigationbarState,
      this.vitualKeyboardState,
      this.vitualKeyboardState.visible
    );
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
    this.emitUpdate();
    return this.state;
  };

  safeAreaSetOverlay = (overlay: boolean) => {
    this.state.overlay = overlay;
    this.emitUpdate();
  };
}

// cd ../plaoc && deno task build:demo && deno task build:server && cd ../desktop-dev && deno task dnt --start install --url http://127.0.0.1:8096/metadata.json
