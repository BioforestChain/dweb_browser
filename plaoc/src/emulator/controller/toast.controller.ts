import { match } from "ts-pattern";
import { createMockModuleServerIpc } from "../helper/mokeServerIpcHelper.ts";
import { BaseController } from "./base-controller.ts";
export class ToastController extends BaseController {
  constructor(readonly show: $Show) {
    super();
  }
  protected _initer = async () => {
    this.emitInit();
    const ipc = await createMockModuleServerIpc("toast.nativeui.browser.dweb");
    ipc
      .onFetch(async (event) => {
        return match(event)
          .with({ pathname: "/show" }, () => {
            const message = event.searchParams.get("message");
            const duration = event.searchParams.get("duration");
            const position = event.searchParams.get("position");
            if (message === null) throw new Error(`message === null`);
            if (duration === null) throw new Error(`duration === null`);
            if (position === null) throw new Error(`position === null`);
            this.show(message, duration, position as "top" | "bottom");
            return Response.json(null);
          })
          .run();
      })
      .forbidden()
      .cors();
    this.emitReady();
  };

  override emitUpdate(): void {
    super.emitUpdate();
  }
}

export interface $Show {
  (message: string, duration: string, position: "top" | "bottom"): void;
}
