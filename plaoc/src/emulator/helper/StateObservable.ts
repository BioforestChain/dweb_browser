import { Ipc, createSignal, mapHelper, simpleEncoder, IpcEvent } from "../../../deps.ts";
import { $Callback, $OffListener } from "../../client/helper/createSignal.ts";


export class StateObservable {
  constructor(readonly getStateJson: () => string) {}

  private _observerIpcMap = new Map<Ipc, $OffListener>();

  private _changeSignal = createSignal();

  private _observe = (cb: $Callback) => this._changeSignal.listen(cb);

  startObserve(ipc: Ipc) {
    mapHelper.getOrPut(this._observerIpcMap, ipc, (ipc) => {
      return this._observe(() => {
        ipc.postMessage(IpcEvent.fromUtf8("observe", simpleEncoder(this.getStateJson(), "utf8")));
      });
    });
  }
  notifyObserver() {
    this._changeSignal.emit();
  }
  stopObserve(ipc: Ipc) {
    console.log("StateObservable.ts 接收到了 stopObserve")
    return mapHelper.getAndRemove(this._observerIpcMap, ipc)?.apply(undefined);
  }
}
