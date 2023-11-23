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

    init() {
        regiserDeskEvent()
    }

    func startUpNMMs(_ app: UIApplication) {
        Main_iosKt.startDwebBrowser(app: app) { Log("Main_iosKt.startDwebBrowser launch: \($1?.localizedDescription ?? "Success")") }
    }

    private func regiserDeskEvent() {
        _ = Main_iosKt.dwebRootUIViewController_setAddHook(addHook(vc:prop:))
        _ = Main_iosKt.dwebRootUIViewController_setUpdateHook(updateHook(prop:))
        _ = Main_iosKt.dwebRootUIViewController_setRemoveHook(removeHook(vcId:))
    }

    private func addHook(vc: UIViewController, prop: HelperPlatformDwebUIViewControllerProperty) -> KotlinUnit {
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            Log("add UIViewController: vcId=\(prop.vcId) zIndex=\(prop.zIndex) visible=\(prop.visible)")
            let pureVc = DwebPureViewController(vc: vc, prop: prop)
            self.vcs[prop.vcId] = pureVc
            
        }
        return KotlinUnit()
    }

    private func updateHook(prop: HelperPlatformDwebUIViewControllerProperty) -> KotlinUnit {
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }

            Log("update UIViewController: vcId=\(prop.vcId) zIndex=\(prop.zIndex) visible=\(prop.visible)")
            if let pureVc = self.vcs.removeValue(forKey: prop.vcId) {
                pureVc.prop = prop
                self.vcs[prop.vcId] = pureVc
            }
        }
        return KotlinUnit()
    }

    private func removeHook(vcId: KotlinInt) -> KotlinUnit {
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }

            Log("remove UIViewController: vcId=\(vcId)")
            vcs.removeValue(forKey: Int32(truncating: vcId))
        }
        return KotlinUnit()
    }
}
