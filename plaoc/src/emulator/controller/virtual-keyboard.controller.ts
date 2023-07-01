export class VirtualKeyboardController {
  constructor() {}

  // 控制显示隐藏
  isShowVirtualKeyboard = false;

  state = {
    insets: {
      top: 0,
      right: 0,
      bottom: 0,
      left: 0,
    },
    overlay: false,
    visible: false,
  };

  private _onUpdate?: () => void;
  onUpdate(cb: () => void) {
    this._onUpdate = cb;
    return this;
  }
  emitUpdate() {
    this._onUpdate?.();
  }

  virtualKeyboardSetOverlay(overlay: boolean) {
    this.state = {
      ...this.state,
      overlay: overlay,
    };
    this.emitUpdate();
    return this;
  }

  virtualKeyboardFirstUpdated() {
    this.state = {
      ...this.state,
      visible: true,
    };
    this.emitUpdate();
  }

  virtualKeyboardHideCompleted() {
    this.isShowVirtualKeyboard = false;
    console.error(`virtualKeybark 隐藏完成了 但是还没有处理`);
  }

  virtualKeyboardShowCompleted() {
    console.error("virutalKeyboard 显示完成了 但是还没有处理");
  }
}
