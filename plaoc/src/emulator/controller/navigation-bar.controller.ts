import { colorToHex, hexaToRGBA, parseQuery, z, zq } from "../../../deps.ts";
import { StateObservable } from "../helper/StateObservable.ts";
import { createMockModuleServerIpc } from "../helper/helper.ts";
import { $BAR_STYLE, $BarState } from "../types.ts";
import { BaseController } from "./base-controller.ts";

export class NavigationBarController extends BaseController {
  private _init = (async () => {
    const ipc = await createMockModuleServerIpc(
      "navigation-bar.nativeui.browser.dweb"
    );
    const query_state = z.object({
      color: zq.transform((color) => colorToHex(JSON.parse(color))).optional(),
      style: z.enum(["DARK", "LIGHT", "DEFAULT"]).optional(),
      overlay: zq.boolean().optional(),
      visible: zq.boolean().optional(),
    });
    ipc
      .onFetch(async (event) => {
        const { pathname, searchParams } = event;
        // 获取导航栏状态
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
      })
      .forbidden()
      .cors();
  })();
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
