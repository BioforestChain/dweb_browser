export class BaseController {
  private _onUpdate?: () => void;

  // Using the Web Animations API
  onUpdate(cb: () => void) {
    this._onUpdate = cb;
    return this;
  }
  // <T>
  emitUpdate() {
    this._onUpdate?.();
  }
}
