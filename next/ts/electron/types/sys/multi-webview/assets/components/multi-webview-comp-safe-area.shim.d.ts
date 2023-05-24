import type { $BarState, $VirtualKeyboardState } from "../../types.js";
export declare function getButtomBarState(navigationBarState: $BarState, isShowVirtualKeyboard: boolean, virtualKeyboardState: $VirtualKeyboardState): {
    visible: boolean;
    overlay: boolean;
    insets: import("../../types.js").$Insets;
};
