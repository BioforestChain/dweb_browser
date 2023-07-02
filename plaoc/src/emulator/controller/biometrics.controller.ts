import { PromiseOut } from "../../../deps.ts";
import { createMockModuleServerIpc } from "../helper/helper.ts";
import { BaseController } from "./base-controller.ts";

export interface $BiometricsReuslt {
  success: boolean;
  message: string;
}

export class BiometricsController extends BaseController {
  private _init = (async () => {
    const ipc = await createMockModuleServerIpc("biometrics.sys.dweb");
    ipc
      .onFetch(async (event) => {
        const { pathname } = event;
        if (pathname === "/check") {
          return Response.json(true);
        }
        if (pathname === "/biometrics") {
          return Response.json(await this.biometricsMock());
        }
      })
      .cros()
      .forbidden();
  })();
  private queue: PromiseOut<$BiometricsReuslt>[] = [];
  get state() {
    return this.queue.at(0);
  }
  biometricsMock() {
    const task = new PromiseOut<$BiometricsReuslt>();
    this.queue.push(task);
    this.emitUpdate();
    task.onFinished(() => {
      this.queue = this.queue.filter((t) => t !== task);
      this.emitUpdate();
    });

    return task.promise;
  }
}
