export class BaseController {
  private _onUpdate?: (controller: this) => void;

  // Using the Web Animations API
  onUpdate(cb: (controller: this) => void) {
    this._onUpdate = cb;
    return this;
  }
  // <T>
  emitUpdate() {
    this._onUpdate?.(this);
  }

  private _onInit?: (controller: this) => void;
  onInit(cb: (controller: this) => void) {
    if (this._inited) {
      cb(this);
    }
    this._onInit = cb;
    return this;
  }
  private _inited = false;
  emitInit() {
    if (this._inited) {
      return;
    }
    this._inited = true;
    this._onInit?.(this);
  }

  private _onReady?: (controller: this) => void;
  onReady(cb: (controller: this) => void) {
    if (this._ready) {
      cb(this);
    }
    this._onReady = cb;
    return this;
  }
  private _ready = false;
  emitReady() {
    if (this._ready) {
      return;
    }
    this._ready = true;
    this._onReady?.(this);
  }
}
