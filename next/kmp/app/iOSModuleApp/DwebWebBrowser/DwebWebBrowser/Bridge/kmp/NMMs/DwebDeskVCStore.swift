//
//  sssss.swift
//  iosApp
//
//  Created by instinct on 2023/11/17.
//  Copyright © 2023 orgName. All rights reserved.
//

// mike todo: import DwebShared
import SwiftUI
import UIKit

// mike todo:
class HelperPlatformDwebUIViewControllerProperty {
    var vcId = 0
    var zIndex = 0
    var visible = false
}

class DwebVCData {
    var vc: UIViewController
    var prop: HelperPlatformDwebUIViewControllerProperty

    init(vc: UIViewController, prop: HelperPlatformDwebUIViewControllerProperty) {
        self.vc = vc
        self.prop = prop
    }
}

class DwebDeskVCStore: ObservableObject {
    static let shared = DwebDeskVCStore()

    @Published var vcs = [DwebVCData]()
    @Published var navgationBarVisible: Visibility = .visible

    init() {
        regiserDeskEvent()
    }

    func startUpNMMs(_ app: UIApplication) {
        #if DEBUG
            let debugMode = true
        #else
            let debugMode = false
        #endif
        // mike todo: Main_iosKt.startDwebBrowser(app: app, debugMode: debugMode) { Log("Main_iosKt.startDwebBrowser launch: \($1?.localizedDescription ?? "Success")") }
    }

    private func regiserDeskEvent() {
        //mike todo: Main_iosKt.dwebViewController.setNavigationBarHook(hook: navigationBarHok(visible:))
        //mike todo: Main_iosKt.dwebViewController.setAddHook(hook: addHook(vc:prop:))
        //mike todo: Main_iosKt.dwebViewController.setUpdateHook(hook: updateHook(prop:))
        //mike todo: Main_iosKt.dwebViewController.setRemoveHook(hook: removeHook(vcId:))
    }

    //mike todo:
//    private func navigationBarHok(visible: KotlinBoolean) {
    private func navigationBarHok(visible: Bool) {
//        navgationBarVisible = visible.boolValue ? .visible : .hidden
        navgationBarVisible = visible ? .visible : .hidden
    }

    private func addHook(vc: UIViewController, prop: HelperPlatformDwebUIViewControllerProperty) {
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            Log("vcId=\(prop.vcId) zIndex=\(prop.zIndex) visible=\(prop.visible)")
            let data = DwebVCData(vc: vc, prop: prop)
            // mike todo: Main_iosKt.dwebViewController.emitOnInit(vcId: prop.vcId)
            self.vcs.append(data)
            self.updateSortVCDatas()
        }
    }

    private func updateHook(prop: HelperPlatformDwebUIViewControllerProperty) {
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            Log("vcId=\(prop.vcId) zIndex=\(prop.zIndex) visible=\(prop.visible)")

            let index = self.vcs.firstIndex { $0.prop.vcId == prop.vcId }

            guard let index = index else {
                assertionFailure("异常的vcId：\(prop.vcId)未找到")
                return
            }

            self.vcs[index].prop = prop
            self.updateSortVCDatas()
        }
    }

    // mike todo: private func removeHook(vcId: KotlinInt) {
    private func removeHook(vcId: Int) {
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            Log("vcId=\(vcId)")
            // mike todo: let vcId = Int32(truncating: vcId)
            vcs.removeAll { $0.prop.vcId == vcId }

            // mike todo: Main_iosKt.dwebViewController.emitOnDestroy(vcId: vcId)
        }
    }

    private func updateSortVCDatas() {
        vcs.sort { $0.prop.zIndex < $1.prop.zIndex }
    }
}
