//
//  KmpBridgeManager.swift
//  iosApp
//
//  Created by instinct on 2023/10/31.
//  Copyright © 2023 orgName. All rights reserved.
//

import Foundation
import DwebShared


class KmpBridgeManager {
    class func registerIMPs() {
        print("iOS registerIMPs")
        KmpNativeBridge.Companion.shared.registerIos(imp: KmpBridgeManager())
    }
    
    var shareController: ShareKmpIMPController?
    
}

extension KmpBridgeManager: SysKmpNativeBridgeInterface {

    func getShareController() -> SysKmpNativeBridgeShareInterface? {
        let imp = shareController ?? ShareKmpIMPController()
        shareController = imp
        return imp
     }
 }

class ShareKmpIMPController: SysKmpNativeBridgeShareInterface {
    
}
