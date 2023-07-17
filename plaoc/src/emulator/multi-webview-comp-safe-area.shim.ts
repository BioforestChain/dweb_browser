// import type { $BarState, $VirtualKeyboardState } from "./types.ts";
import type { $Insets } from "../client/util/insets.ts";
import type { $VirtualKeyboardState } from "./types.ts";

export function getButtomBarState(
  // navigationBarState: $BarState,
  navigationBarState: {
    overlay: boolean;
    visible: boolean;
    insets: $Insets;
  },
  isShowVirtualKeyboard: boolean,
  virtualKeyboardState: $VirtualKeyboardState
) {
  return {
    visible: isShowVirtualKeyboard ? virtualKeyboardState.visible : navigationBarState.visible,
    overlay: isShowVirtualKeyboard ? virtualKeyboardState.overlay : navigationBarState.overlay,
    insets: isShowVirtualKeyboard ? virtualKeyboardState.insets : navigationBarState.insets,
  };
}
