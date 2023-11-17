//
//  sssss.swift
//  iosApp
//
//  Created by instinct on 2023/11/17.
//  Copyright © 2023 orgName. All rights reserved.
//

import UIKit
import DwebShared

class DwebDeskVCStroe: ObservableObject {
    
    static let shared = DwebDeskVCStroe()
    
    @Published var vc: DwebRootUIViewController? = nil
    
    init() {
        regiserDeskEvent()
    }
    
    func startUpNMMs(_ app: UIApplication) {
        Main_iosKt.startDwebBrowser(app: app) { Log("Main_iosKt.startDwebBrowser launch: \($1?.localizedDescription ?? "Success")")}
    }
    
    private func regiserDeskEvent() {
        _ = Main_iosKt.dwebRootUIViewController_setCreateHook(composeDeskVCCreate(id:))
        _ = Main_iosKt.dwebRootUIViewController_setUpdateHook(composeDeskVCUpdate(vc:))
    }
    
    private func composeDeskVCCreate(id: KotlinInt) -> KotlinUnit {
        Log("")
        vc = DwebRootUIViewController(vcId: Int(truncating: id))
        return KotlinUnit()
    }
    
    private func composeDeskVCUpdate(vc: UIViewController) -> KotlinUnit {
        Log("")
        guard let vc = vc as? DwebRootUIViewController else { return KotlinUnit() }
        self.vc = vc
        return KotlinUnit()
    }
}
