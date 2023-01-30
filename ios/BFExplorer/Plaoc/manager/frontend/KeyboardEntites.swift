//
//  KeyboardEntites.swift
//  BFExplorer
//
//  Created by ui03 on 2022/12/23.
//

import Foundation

extension PlaocHandleModel {
    //键盘安全区域
    func keyboardSafeArea(param: Any) -> UIEdgeInsets {
        return controller?.keyboardSafeArea ?? .zero
    }
    //键盘高度
    func keyboardHeight(param: Any) -> CGFloat {
        return controller?.keyboardHeight ?? 0
    }
    //显示键盘
    func showKeyboard(param: Any) -> Bool {
        return true
    }
    //隐藏键盘
    func hideKeyBoard(param: Any) -> Bool {
        controller?.view.endEditing(true)
        return true
    }
    //键盘是否overlay
    func keyboardOverlay(param: Any) -> Bool {
        return controller?.isKeyboardOverlay() ?? false
    }
    //设置键盘overlay
    func updateKeyboardOverlay(param: Any) -> Bool {
        guard let param = param as? Bool else { return false }
        controller?.setKeyboardOverlay(overlay: param)
        return true
    }
}
