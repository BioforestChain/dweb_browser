import { colorToHex, hexaToRGBA, parseQuery, z } from "../../../deps.ts";
import { createStreamIpc, fetchResponse } from "../helper.ts";
import { $BAR_STYLE, $BarState } from "../types.ts";
export class StatusBarController {
  constructor() {
    void this._init();
  }
  private async _init() {
    const ipc = await createStreamIpc("status-bar.nativeui.browser.dweb");
    const query_state = z.object({
      color: z.string().optional(),
      style: z.enum(["DARK", "LIGHT", "DEFAULT"]).optional(),
      overlay: z.boolean().optional(),
      visible: z.boolean().optional(),
    });

    ipc.onFetch(async (event) => {
      const { pathname, searchParams } = event;
      // 获取状态栏状态
      if (pathname.endsWith("/getState")) {
        return Response.json(await this.statusBarGetState());
      }
      if (pathname.endsWith("/setState")) {
        const { color, ...states } = parseQuery(searchParams, query_state);

        this.statusBarSetState({
          color: color && colorToHex(JSON.parse(color)),
          ...states,
        });
        return Response.json(null);
      }
      // 开始订阅数据
      if (pathname.endsWith("/startObserve")) {
        return Response.json("");
      }
      // 开始订阅数据
      if (pathname.endsWith("/startObserve")) {
        return Response.json("");
      }
      // 开始订阅数据
      if (pathname.endsWith("/startObserve")) {
        return Response.json("");
      }
      return fetchResponse.FORBIDDEN();
    });
  }

  state: $BarState = {
    color: "#FFFFFFFF",
    style: "DEFAULT",
    set insets(_v) {
      console.error(new Error("fffff"), _v);
      debugger;
    },
    get insets() {
      return {
        top: 38,
        right: 0,
        bottom: 0,
        left: 0,
      };
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

  async statusBarGetState() {
    return {
      ...this.state,
      color: await hexaToRGBA(this.state.color),
    };
  }
}
