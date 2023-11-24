//
//  sssss.swift
//  iosApp
//
//  Created by instinct on 2023/11/17.
//  Copyright © 2023 orgName. All rights reserved.
//

import DwebShared
import UIKit

class DwebDeskVCStroe: ObservableObject {
    static let shared = DwebDeskVCStroe()

    @Published var vcs = [Int32: DwebPureViewController]()

    func getSortedVcs() -> [UIViewController] {
        return Array(vcs.values.sorted(by: {
            $1.prop.zIndex < $0.prop.zIndex
        }).map {
            $0.vc
        })
    }

    init() {
        regiserDeskEvent()
    }

    func startUpNMMs(_ app: UIApplication) {
        Main_iosKt.startDwebBrowser(app: app) { Log("Main_iosKt.startDwebBrowser launch: \($1?.localizedDescription ?? "Success")") }
    }

    private func regiserDeskEvent() {
        Main_iosKt.dwebViewController.setAddHook(hook: addHook(vc:prop:))
        Main_iosKt.dwebViewController.setUpdateHook(hook: updateHook(prop:))
        Main_iosKt.dwebViewController.setRemoveHook(hook: removeHook(vcId:))
    }

    private func addHook(vc: UIViewController, prop: HelperPlatformDwebUIViewControllerProperty) -> Void {
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            Log("add UIViewController: vcId=\(prop.vcId) zIndex=\(prop.zIndex) visible=\(prop.visible)")
            let pureVc = DwebPureViewController(vc: vc, prop: prop)
            self.vcs[prop.vcId] = pureVc
        }
    }

    private func updateHook(prop: HelperPlatformDwebUIViewControllerProperty) -> Void {
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }

            Log("update UIViewController: vcId=\(prop.vcId) zIndex=\(prop.zIndex) visible=\(prop.visible)")
            if let pureVc = self.vcs.removeValue(forKey: prop.vcId) {
                pureVc.prop = prop
                self.vcs[prop.vcId] = pureVc
            }
        }
    }

    private func removeHook(vcId: KotlinInt) -> Void {
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }

            Log("remove UIViewController: vcId=\(vcId)")
            vcs.removeValue(forKey: Int32(truncating: vcId))
        }
    }
}
