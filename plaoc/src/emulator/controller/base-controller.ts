export class BaseController {
  private _onUpdate?: () => void;
  onUpdate(cb: () => void) {
    this._onUpdate = cb;
    return this;
  }
  emitUpdate() {
    this._onUpdate?.();
  }
}
