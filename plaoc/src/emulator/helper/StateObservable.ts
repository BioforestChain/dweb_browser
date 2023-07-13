import { Ipc, createSignal, mapHelper, simpleEncoder } from "../../../deps.ts";
import { $Callback, $OffListener } from "../../client/helper/createSignal.ts";

export class StateObservable {
  constructor(readonly getStateJson: () => string) {}

  private _observerIpcMap = new Map<Ipc, $OffListener>();

  private _changeSignal = createSignal();

  private _observe = (cb: $Callback) => this._changeSignal.listen(cb);

  startObserve(ipc: Ipc) {
    mapHelper.getOrPut(this._observerIpcMap, ipc, (ipc) => {
      return this._observe(() => {
        if (this.controller) {
          this.controller.enqueue(simpleEncoder(this.getStateJson(), "utf8"));
        }
        // ipc.postMessage(IpcEvent.fromUtf8("observe", simpleEncoder(this.getStateJson(), "utf8")));
      });
    });
  }
  notifyObserver() {
    this._changeSignal.emit();
  }
  stopObserve(ipc: Ipc) {
    console.log("", "stopObserve");
    if (this.controller) {
      console.log("close");
      this.controller.close();
    }
    return mapHelper.getAndRemove(this._observerIpcMap, ipc)?.apply(undefined);
  }

  controller: ReadableStreamDefaultController | undefined;
  observe(controller: ReadableStreamDefaultController) {
    console.log("", "observe");
    this.controller = controller;
  }
}
