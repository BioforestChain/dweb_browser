import { Ipc, createSignal, mapHelper, simpleEncoder } from "../../../deps.ts";
import { $Callback, $OffListener } from "../../client/helper/createSignal.ts";

export class StateObservable {
  constructor(readonly getStateJson: () => string) {}

  private _observerIpcMap = new Map<Ipc, $OffListener>();

  private _changeSignal = createSignal();

  private _observe = (cb: $Callback) => this._changeSignal.listen(cb);

  private _controllersMap = new Map<Ipc, ReadableStreamDefaultController>();

  startObserve(ipc: Ipc, controller: ReadableStreamDefaultController) {
    this._controllersMap.set(ipc, controller);
    mapHelper.getOrPut(this._observerIpcMap, ipc, () => {
      return this._observe(() => {
        controller?.enqueue(simpleEncoder(this.getStateJson() + "\n", "utf8"));
      });
    });
  }
  notifyObserver() {
    this._changeSignal.emit();
  }
  stopObserve(ipc: Ipc) {
    const controller = this._controllersMap.get(ipc);
    if (controller === undefined) throw new Error(`controller === undefined`);
    controller.close();
    this._controllersMap.delete(ipc);
    return mapHelper.getAndRemove(this._observerIpcMap, ipc)?.apply(undefined);
  }
}
