export function getButtomBarState(navigationBarState, isShowVirtualKeyboard, virtualKeyboardState) {
    return {
        visible: isShowVirtualKeyboard ? virtualKeyboardState.visible : navigationBarState.visible,
        overlay: isShowVirtualKeyboard ? virtualKeyboardState.overlay : navigationBarState.overlay,
        insets: isShowVirtualKeyboard ? virtualKeyboardState.insets : navigationBarState.insets
    };
}
