//
//  KmpBridgeManager.swift
//  iosApp
//
//  Created by bfs-kingsword09 on 2023/12/21.
//  Copyright © 2023 orgName. All rights reserved.
//

import Foundation

import Foundation
import DwebShared
import Combine


class KmpBridgeManager {
    
    static let shared = KmpBridgeManager()
    
    func registerIMPs() {
        KmpNativeBridge.Companion.shared.registerIos(imp: self)
        DwebBrowserIosSupport().registerIosService(imp: DwebBrowserIosIMP.shared)
    }
}

extension KmpBridgeManager: KmpNativeBridgeInterface {}
