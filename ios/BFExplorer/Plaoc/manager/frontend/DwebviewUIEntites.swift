//
//  DwebviewUIEntites.swift
//  BFExplorer
//
//  Created by ui08 on 2023/1/6.
//

import Foundation
import SwiftyJSON

extension PlaocHandleModel {
    func executiveDwebviewUI(param: Any) -> String {
        var result: Any = "no function param.";
        guard let param = param as? String else { return "{cmd: error, data: \(result)}"  }
        let str = param.hexStringToString(symbol: ",")
        let data = JSON(parseJSON: str)
        
        if data["function"].exists() {
        switch(data["function"].stringValue) {
            case "SetStatusBarBackgroundColor":
                result = updateStatusBarBackgroundColor(param: data["data"].stringValue)
            case "GetStatusBarBackgroundColor":
                result = statusBarColor(param: data["data"])
            case "GetStatusBarIsDark":
                result = statusBarIsDark(param: data["data"])
            case "GetStatusBarVisible":
                result = statusBarVisible(param: data["data"])
            case "SetStatusBarVisible":
                result = updateStatusBarVisible(param: data["data"].boolValue)
            case "GetStatusBarOverlay":
                result = statusBarOverlay(param: data["data"])
            case "SetStatusBarOverlay":
                result = updateStatusBarOverlay(param: data["data"].boolValue)
            case "SetStatusBarStyle":
                result = updateStatusBarStyle(param: data["data"].stringValue)
            case "GetKeyBoardSafeArea":
                result = keyboardSafeArea(param: data["data"])
            case "GetKeyBoardHeight":
                result = keyboardHeight(param: data["data"])
            case "GetKeyBoardOverlay":
                result = keyboardOverlay(param: data["data"])
            case "SetKeyBoardOverlay":
                result = updateKeyboardOverlay(param: data["data"].boolValue)
            case "ShowKeyBoard":
                result = showKeyboard(param: data["data"])
            case "HideKeyBoard":
                result = hideKeyBoard(param: data["data"])
            case "TopBarNavigationBack":
                result = topBarNavigationBack(param: data["data"])
            case "GetTopBarShow":
                result = topbarShow(param: data["data"])
            case "SetTopBarShow":
                result = updateTopBarShow(param: data["data"].boolValue)
            case "GetTopBarOverlay":
                result = topBarOverlay(param: data["data"])
            case "SetTopBarOverlay":
                result = updateTopBarOverlay(param: data["data"].boolValue)
            case "GetTopBarAlpha":
                result = topBarAlpha(param: data["data"])
            case "SetTopBarAlpha":
                result = updateTopBarAlpha(param: data["data"].boolValue)
            case "GetTopBarTitle":
                result = topBarTitle(param: data["data"])
            case "SetTopBarTitle":
                result = updateTopBarTitle(param: data["data"].stringValue)
            case "HasTopBarTitle":
                result = isTopBarTitle(param: data["data"])
            case "GetTopBarHeight":
                result = topBarHeight(param: data["data"])
            case "GetTopBarActions":
                result = topBarActions(param: data["data"])
            case "SetTopBarActions":
                result = updateTopBarActions(param: data["data"].stringValue)
            case "GetTopBarBackgroundColor":
                result = topBarBackgroundColor(param: data["data"])
            case "SetTopBarBackgroundColor":
                result = updateTopBarBackgroundColor(param: data["data"].stringValue)
            case "GetTopBarForegroundColor":
                result = topBarForegroundColor(param: data["data"])
            case "SetTopBarForegroundColor":
                result = updateTopBarForegroundColor(param: data["data"].stringValue)
            case "GetBottomBarEnabled":
                result = bottomBarShow(param: data["data"])
            case "SetBottomBarEnabled":
                result = updateBottomBarShow(param: data["data"].boolValue)
            case "GetBottomBarAlpha":
                result = bottomBarAlpha(param: data["data"])
            case "SetBottomBarAlpha":
                result = updateBottomBarAlpha(param: data["data"].stringValue)
            case "GetBottomBarHeight":
                result = bottomBarHeight(param: data["data"])
            case "SetBottomBarHeight":
                result = updateBottomBarHeight(param: data["data"].stringValue)
            case "GetBottomBarActions":
                result = bottomBarActions(param: data["data"])
            case "SetBottomBarActions":
                result = updateBottomBarActions(param: data["data"].stringValue)
            case "GetBottomBarBackgroundColor":
                result = bottomBarBackgroundColor(param: data["data"])
            case "SetBottomBarBackgroundColor":
                result = updateBottomBarBackgroundColor(param: data["data"].stringValue)
            case "GetBottomBarForegroundColor":
                result = bottomBarForegroundColor(param: data["data"])
            case "SetBottomBarForegroundColor":
                result = updatebottomBarForegroundColor(param: data["data"].stringValue)
            case "OpenDialogAlert":
                result = OpenDialogAlert(param: data["data"].stringValue)
            case "OpenDialogPrompt":
                result = OpenDialogPrompt(param: data["data"].stringValue)
            case "OpenDialogConfirm":
                result = OpenDialogConfirm(param: data["data"].stringValue)
            case "OpenDialogWarning":
                result = OpenDialogWarning(param: data["data"].stringValue)
            default:
                result = "SetDWebViewUI: no function match value."
            }
        } else {
            result = "SetDWebViewUI: no function param."
        }
        
        return "{ \"cmd\": \"\(data["function"].stringValue)\", \"data\": \(result) }"
    }
}

