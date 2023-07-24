import { match } from "ts-pattern";
import { parseQuery, z, zq } from "../../../deps.ts";
import { StateObservable } from "../helper/StateObservable.ts";
import { createMockModuleServerIpc } from "../helper/mokeServerIpcHelper.ts";
import { BaseController } from "./base-controller.ts";

export class VirtualKeyboardController extends BaseController {
  private _init = (async () => {
    this.emitInit();
    const ipc = await createMockModuleServerIpc("virtual-keyboard.nativeui.browser.dweb");
    const query_state = z.object({
      overlay: zq.boolean().optional(),
      visible: zq.boolean().optional(),
    });
    ipc
      .onFetch(async (event) => {
        return match(event)
          .with({ pathname: "/getState" }, () => {
            return Response.json(this.state);
          })
          .with({ pathname: "/setState" }, (event) => {
            const states = parseQuery(event.searchParams, query_state);
            this.virtualKeyboardSeVisiable(states.visible === true ? true : false);
            this.virtualKeyboardSetOverlay(states.overlay);
            return Response.json(true);
          })
          .with({ pathname: "/observe" }, () => {
            const readableStream = new ReadableStream({
              start: (controller) => {
                this.observer.startObserve(ipc, controller);
              },
              pull() {},
              cancel() {},
            });
            return new Response(readableStream, {
              status: 200,
              statusText: "ok",
              headers: new Headers({ "Content-Type": "application/octet-stream" }),
            });
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

  override emitUpdate = (): void => {
    this.observer.notifyObserver();
    super.emitUpdate();
  };

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

  virtualKeyboardSetOverlay = (overlay = true) => {
    this.state = {
      ...this.state,
      overlay: overlay,
    };
    this.emitUpdate();
  };

  virtualKeyboardSeVisiable = (visible = true) => {
    this.state = {
      ...this.state,
      visible: visible,
    };
    visible ? (this.isShowVirtualKeyboard = visible) : "";
    this.emitUpdate();
  };

  virtualKeyboardFirstUpdated = (e: Event) => {
    this.state.insets.bottom = this.getHeightByEvent(e);
    this.state.visible = true;
    this.emitUpdate();
  };

  virtualKeyboardHideCompleted = (e: Event) => {
    this.isShowVirtualKeyboard = false;
    // 需要显示 navigationbar;
    // console.error(`virtualKeybark 隐藏完成了 但是还没有处理`);
    this.state.insets.bottom = this.getHeightByEvent(e);
    this.state.visible = false;
    this.emitUpdate();
  };

  virtualKeyboardShowCompleted = (e: Event) => {
    this.state.insets.bottom = this.getHeightByEvent(e);
    this.state.visible = true;
    this.emitUpdate();
  };

  getHeightByEvent = (e: Event) => {
    const virtualKeyboardEl: HTMLElement | null = e.target as HTMLElement;
    if (virtualKeyboardEl === null) throw new Error("vitualKeyboardEl === null");
    const rect = virtualKeyboardEl.getBoundingClientRect();
    return Math.ceil(rect.height);
  };
}
