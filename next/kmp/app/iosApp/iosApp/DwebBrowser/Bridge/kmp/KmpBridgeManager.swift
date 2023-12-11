//
//  KmpBridgeManager.swift
//  iosApp
//
//  Created by instinct on 2023/10/31.
//  Copyright © 2023 orgName. All rights reserved.
//

import Foundation
import DwebShared
import Combine


class KmpBridgeManager {
    
    static let shared = KmpBridgeManager()
        
    private(set) var eventPublisher = PassthroughSubject<KmpEvent, Never>()
    
    func registerIMPs() {
        KmpNativeBridge.Companion.shared.registerIos(imp: self)
        DwebBrowserIosSupport().registerIosService(imp: DwebBrowserIosIMP.shared)
    }
}


extension KmpBridgeManager: SysKmpNativeBridgeInterface {

    func invokeKmpEvent(event: SysKmpToIosEvent) -> Any? {
        Log("\(event)")
        let e = KmpEvent(name: event.name, inputDatas: event.inputDatas)
        switch event.name {
        case KmpEvent.colorScheme:
            eventPublisher.send(e)
        default:
            assert(false, "no response action for: \(event.name) \(event.inputDatas?.description ?? "")")
        }
        return nil
    }
    
    func invokeAsyncKmpEvent(event: SysKmpToIosEvent) async throws -> Any? {
        Log("\(event)")
        let e = KmpEvent(name: event.name, inputDatas: event.inputDatas)
        switch event.name {
        case KmpEvent.share:
            return await KmpEventShareResposeAction(e).doAction()
        default:
            assert(false, "no response action for: \(event.name) \(event.inputDatas?.description ?? "")")
        }
        return nil
    }
 }
