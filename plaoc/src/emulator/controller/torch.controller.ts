import { createMockModuleServerIpc } from "../helper/mokeServerIpcHelper.ts";
import { BaseController } from "./base-controller.ts";

export class TorchController extends BaseController {
  protected _initer = async () => {
    this.emitInit();
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
      .forbidden()
      .cors();
    this.emitReady();
  };

  state = { isOpen: false };

  torchToggleTorch() {
    this.state = {
      isOpen: !this.state.isOpen,
    };
    this.emitUpdate();
    return this.state.isOpen;
  }
}
