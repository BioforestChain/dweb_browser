//
//  KmpBridgeManager.swift
//  iosApp
//
//  Created by bfs-kingsword09 on 2023/12/21.
//  Copyright © 2023 orgName. All rights reserved.
//

import Foundation
// mike todo: import DwebShared
import Combine


class KmpBridgeManager {
    
    static let shared = KmpBridgeManager()
    
    func registerIMPs() {
        // mike todo:         KmpNativeBridge.Companion.shared.registerIos(imp: self)
        // mike todo:         DwebBrowserIosSupport().registerIosService(imp: DwebBrowserIosIMP.shared)
    }
}

// mike todo: extension KmpBridgeManager: KmpNativeBridgeInterface {}
