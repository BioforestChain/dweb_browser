import type { $BarState, $VirtualKeyboardState } from "../types.js";

export function getButtomBarState(
  navigationBarState: $BarState,
  isShowVirtualKeyboard: boolean,
  virtualKeyboardState: $VirtualKeyboardState,
){
  return {
    visible: isShowVirtualKeyboard ? virtualKeyboardState.visible : navigationBarState.visible,
    overlay: isShowVirtualKeyboard ? virtualKeyboardState.overlay : navigationBarState.overlay,
    insets: isShowVirtualKeyboard ? virtualKeyboardState.insets : navigationBarState.insets
  }
}