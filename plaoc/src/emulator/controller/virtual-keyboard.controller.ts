import { parseQuery, z, zq } from "../../../deps.ts";
import { StateObservable } from "../helper/StateObservable.ts";
import { createMockModuleServerIpc } from "../helper/mokeServerIpcHelper.ts";
import { BaseController } from "./base-controller.ts";

export class VirtualKeyboardController extends BaseController {
  private _init = (async () => {
    this.emitInit();
    const ipc = await createMockModuleServerIpc(
      "virtual-keyboard.nativeui.browser.dweb"
    );
    const query_state = z.object({
      overlay: zq.boolean().optional(),
      visible: zq.boolean().optional(),
    });
    ipc
      .onFetch(async (event) => {
        const { pathname, searchParams } = event;
        // 获取虚拟键盘状态
        if (pathname.endsWith("/getState")) {
          return Response.json(this.state);
        }
        if (pathname.endsWith("/setState")) {
          const states = parseQuery(searchParams, query_state);
          this.virtualKeyboardSeVisiable(states.visible);
          this.virtualKeyboardSetOverlay(states.overlay);
          return Response.json(true);
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
    this.emitReady();
  })();
  observer = new StateObservable(() => {
    return JSON.stringify(this.state);
  });

  override emitUpdate(): void {
    this.observer.notifyObserver();
    super.emitUpdate();
  }

  // 控制显示隐藏
  isShowVirtualKeyboard = false;

  state = {
    insets: {
      top: 0,
      right: 0,
      bottom: 0,
      left: 0,
    },
    overlay: false,
    visible: false,
  };

  virtualKeyboardSetOverlay(overlay = true) {
    this.state = {
      ...this.state,
      overlay: overlay,
    };
    this.emitUpdate();
  }

  virtualKeyboardSeVisiable(visible = true) {
    this.state = {
      ...this.state,
      visible: visible,
    };
    this.emitUpdate();
  }

  virtualKeyboardFirstUpdated() {
    this.state = {
      ...this.state,
      visible: true,
    };
    this.emitUpdate();
  }

  virtualKeyboardHideCompleted() {
    this.isShowVirtualKeyboard = false;
    console.error(`virtualKeybark 隐藏完成了 但是还没有处理`);
  }

  virtualKeyboardShowCompleted() {
    console.error("virutalKeyboard 显示完成了 但是还没有处理");
  }
}
