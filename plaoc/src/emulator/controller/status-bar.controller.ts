import { colorToHex, hexaToRGBA, parseQuery, zq } from "../../../deps.ts";
import { StateObservable } from "../helper/StateObservable.ts";
import { createStreamIpc } from "../helper/helper.ts";
import { $BAR_STYLE, $BarState } from "../types.ts";
export class StatusBarController {
  constructor() {
    void this._init();
  }
  private async _init() {
    const ipc = await createStreamIpc("status-bar.nativeui.browser.dweb");
    const query_state = zq.object({
      color: zq.transform((color) => colorToHex(JSON.parse(color))).optional(),
      style: zq.enum(["DARK", "LIGHT", "DEFAULT"]).optional(),
      overlay: zq.boolean().optional(),
      visible: zq.boolean().optional(),
    });

    ipc
      .onFetch((event) => {
        const { pathname, searchParams, ipc } = event;
        // 获取状态栏状态
        if (pathname.endsWith("/getState")) {
          const state = this.statusBarGetState();
          console.log(state);
          return Response.json(state);
        }
        if (pathname.endsWith("/setState")) {
          const states = parseQuery(searchParams, query_state);
          this.statusBarSetState(states);
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
      .cros()
      .noFound();
  }

  observer = new StateObservable(() => {
    return JSON.stringify(this.state);
  });

  state: $BarState = {
    color: "#FFFFFFFF",
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

  private _onUpdate?: () => void;
  onUpdate(cb: () => void) {
    this._onUpdate = cb;
    return this;
  }
  emitUpdate() {
    // this.observer.notifyObserver();
    console.log("state=>", this.state);
    this._onUpdate?.();
  }
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
