//
//  sssss.swift
//  DwebBrowser
//
//  Created by instinct on 2023/11/17.
//  Copyright © 2023 orgName. All rights reserved.
//

import DwebShared
import Observation
import SwiftUI
import UIKit

class DwebVCData {
    var vc: UIViewController
    var prop: HelperComposeDwebUIViewControllerProperty

    init(vc: UIViewController, prop: HelperComposeDwebUIViewControllerProperty) {
        self.vc = vc
        self.prop = prop
    }
}

@Observable class DwebDeskVCStore {
    var vcs = [DwebVCData]()
    var navgationBarVisible: Visibility = .visible
    var shouldEnableEdgeSwipe: Bool = false
    init() {
        regiserDeskEvent()
    }

    class func startUpNMMs(_ app: UIApplication) {
        #if DEBUG
            let debugMode = true//,":verbose:/.+/"
        #else
            let debugMode = false
        #endif
        Main_iosKt.startDwebBrowser(app: app, debugMode: debugMode, debugTags: ["/.+/"]) { Log("Main_iosKt.startDwebBrowser launch: \($1?.localizedDescription ?? "Success")") }
    }

    private func regiserDeskEvent() {
        Main_iosKt.dwebViewController.setNavigationBarHook(hook: navigationBarHok(visible:))
        Main_iosKt.dwebViewController.setAddHook(hook: addHook(vc:prop:))
        Main_iosKt.dwebViewController.setUpdateHook(hook: updateHook(prop:))
        Main_iosKt.dwebViewController.setRemoveHook(hook: removeHook(vcId:))
        Main_iosKt.dwebViewController.setUpdateSwitchHook(cb: updateSwitch(enable:))
    }

    private func updateSwitch(enable: KotlinBoolean) {
        shouldEnableEdgeSwipe = enable.boolValue
    }
    
    private func navigationBarHok(visible: KotlinBoolean) {
        navgationBarVisible = visible.boolValue ? .visible : .hidden
    }

    private func addHook(vc: UIViewController, prop: HelperComposeDwebUIViewControllerProperty) {
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            Log("vcId=\(prop.vcId) zIndex=\(prop.zIndex) visible=\(prop.visible)")

            if let data = self.vcs.first(where: { data in data.vc == vc }) {
                updateHook(
                    prop: HelperComposeDwebUIViewControllerProperty(
                        vcId: data.prop.vcId, zIndex: prop.zIndex, visible: prop.visible, fullscreen: prop.fullscreen))
                return
            }

            let data = DwebVCData(vc: vc, prop: prop)
            self.vcs.append(data)
            self.updateSortVCDatas()
            Main_iosKt.dwebViewController.emitOnInit(vcId: prop.vcId)
        }
    }

    private func updateHook(prop: HelperComposeDwebUIViewControllerProperty) {
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            Log("vcId=\(prop.vcId) zIndex=\(prop.zIndex) visible=\(prop.visible)")

            let index = self.vcs.firstIndex { $0.prop.vcId == prop.vcId }

            guard let index = index else {
                print("ERROR: 异常的vcId：\(prop.vcId)未找到")
                return
            }

            self.vcs[index].prop = prop
            self.updateSortVCDatas()
        }
    }

    private func removeHook(vcId: KotlinInt) {
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            Log("vcId=\(vcId)")
            let vcId = Int32(truncating: vcId)
            vcs.removeAll { $0.prop.vcId == vcId }

            Main_iosKt.dwebViewController.emitOnDestroy(vcId: vcId)
        }
    }

    private func updateSortVCDatas() {
        vcs.sort { $0.prop.zIndex < $1.prop.zIndex }
    }
}
