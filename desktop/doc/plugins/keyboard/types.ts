export type $Keyboard = typeof import('./keyboard.capacitor')['default'];
/**
 * 虚拟键盘状态集
 */
export const enum $KEYBOARD_STATUS {
  WILL_SHOW,
  DID_SHOW,
  WILL_HIDE,
  DID_HIDE,
}

/**
 * 虚拟键盘信息
 */
export interface $KeyboardInfo {
  /** 当前的状态 */
  status: $KEYBOARD_STATUS;
  /** 当前的高度 */
  height: number;
}
