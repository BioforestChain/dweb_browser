import { createMockModuleServerIpc } from "../helper/helper.ts";

export class TorchController {
  constructor() {
    void this._init();
  }
  private async _init() {
    const ipc = await createMockModuleServerIpc("torch.nativeui.browser.dweb");
    ipc
      .onFetch(async (event) => {
        const { pathname } = event;
        // 打开关闭手电筒
        if (pathname === "/toggleTorch") {
          this.torchToggleTorch();
          return Response.json(true);
        }
        // 手电筒状态
        if (pathname === "/state") {
          return Response.json(this.state.isOpen);
        }
      })
      .cros()
      .forbidden();
  }

  state = { isOpen: false };

  private _onUpdate?: () => void;
  onUpdate(cb: () => void) {
    this._onUpdate = cb;
    return this;
  }
  emitUpdate() {
    this._onUpdate?.();
  }

  torchToggleTorch() {
    this.state = {
      isOpen: !this.state.isOpen,
    };
    this.emitUpdate();
    return this.state.isOpen;
  }
}
