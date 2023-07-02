import { colorToHex, hexaToRGBA, parseQuery, z } from "../../../deps.ts";
import { StateObservable } from "../helper/StateObservable.ts";
import { createStreamIpc, fetchResponse } from "../helper/helper.ts";
import { $BAR_STYLE, $BarState } from "../types.ts";

export class NavigationBarController {
  constructor() {
    void this._init();
  }
  private async _init() {
    const ipc = await createStreamIpc("navigation-bar.nativeui.browser.dweb");
    const query_state = z.object({
      color: z
        .string()
        .transform((color) => colorToHex(JSON.parse(color)))
        .optional(),
      style: z.enum(["DARK", "LIGHT", "DEFAULT"]).optional(),
      overlay: z
        .string()
        .transform((overlay) => overlay === "true")
        .optional(),
      visible: z
        .string()
        .transform((visible) => visible === "true")
        .optional(),
    });
    ipc.onFetch(async (event) => {
      const { pathname, searchParams } = event;
      // 获取状态栏状态
      if (pathname.endsWith("/getState")) {
        const state = await this.navigationBarGetState();
        return Response.json(state);
      }
      if (pathname.endsWith("/setState")) {
        const states = parseQuery(searchParams, query_state);
        this.navigationBarSetState(states);
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
      return fetchResponse.FORBIDDEN();
    });
  }
  observer = new StateObservable(() => {
    return JSON.stringify(this.state);
  });
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

  private _onUpdate?: () => void;
  onUpdate(cb: () => void) {
    this._onUpdate = cb;
    return this;
  }
  emitUpdate() {
    console.log("navigaion update=>", this.state);
    this._onUpdate?.();
  }

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

  async navigationBarGetState() {
    return {
      ...this.state,
      color: hexaToRGBA(this.state.color),
    };
  }
}
