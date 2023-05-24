"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.getButtomBarState = void 0;
function getButtomBarState(navigationBarState, isShowVirtualKeyboard, virtualKeyboardState) {
    return {
        visible: isShowVirtualKeyboard ? virtualKeyboardState.visible : navigationBarState.visible,
        overlay: isShowVirtualKeyboard ? virtualKeyboardState.overlay : navigationBarState.overlay,
        insets: isShowVirtualKeyboard ? virtualKeyboardState.insets : navigationBarState.insets
    };
}
exports.getButtomBarState = getButtomBarState;
