//
//  TopBarEntites.swift
//  BFExplorer
//
//  Created by ui03 on 2022/12/23.
//

import Foundation

extension PlaocHandleModel {
    //顶部栏返回
    func topBarNavigationBack(param: Any) -> Bool {
        return true
    }
    //顶部栏是否显示
    func topbarShow(param: Any) -> Bool {
        return controller?.getNaviHiddenState() ?? true
    }
    //设置顶部栏是否显示
    func updateTopBarShow(param: Any) -> Bool {
        guard let param = param as? Bool else { return false }
        controller?.hiddenNavigationBar(isHidden: param)
        return true
    }
    //状态栏透明度
    func topBarAlpha(param: Any) -> CGFloat {
        return controller?.naviViewAlpha() ?? 0
    }
    //设置状态栏透明度
    func updateTopBarAlpha(param: Any) -> Bool {
        guard let param = param as? String, Float(param) != nil else { return false }
        controller?.setNaviViewAlpha(alpha: CGFloat(Float(param)!))
        return true
    }
    //顶部栏是否overlay
    func topBarOverlay(param: Any) -> Bool {
        return controller?.naviViewOverlay() ?? true
    }
    //设置顶部栏overlay
    func updateTopBarOverlay(param: Any) -> Bool {
        guard let param = param as? Bool else { return false }
        controller?.updateNavigationBarOverlay(overlay: param)
        return true
    }
    //顶部栏标题
    func topBarTitle(param: Any) -> String {
        return controller?.titleString() ?? ""
    }
    //设置顶部栏标题
    func updateTopBarTitle(param: Any) -> Bool {
        guard let param = param as? String else { return false }
        controller?.setNaviViewTitle(title: param)
        return true
    }
    //顶部栏是否标题
    func isTopBarTitle(param: Any) -> Bool {
        return controller?.isNaviTitleExit() ?? false
    }
    //顶部栏高度
    func topBarHeight(param: Any) -> CGFloat {
        return controller?.naviViewHeight() ?? 0
    }
    //顶部栏按钮
    func topBarActions(param: Any) -> String {
        return controller?.naviActions() ?? ""
    }
    //设置顶部栏按钮
    func updateTopBarActions(param: Any) -> Bool {
        guard let param = param as? String else { return false }
        controller?.setNaviButtons(content: param)
        return true
    }
    //顶部栏背景色
    func topBarBackgroundColor(param: Any) -> String {
        return controller?.naviViewBackgroundColor() ?? ""
    }
    //设置顶部栏背景色
    func updateTopBarBackgroundColor(param: Any) -> Bool {
        guard let param = param as? String else { return false }
        controller?.updateNavigationBarBackgroundColor(colorString: param)
        return true
    }
    //顶部栏前景色
    func topBarForegroundColor(param: Any) -> String {
        return controller?.naviViewForegroundColor() ?? ""
    }
    //设置顶部栏前景色
    func updateTopBarForegroundColor(param: Any) -> Bool {
        guard let param = param as? String else { return false }
        controller?.updateNavigationBarTintColor(colorString: param)
        return true
    }
}
