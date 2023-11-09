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
    private var events = [KmpEvent]()
    
    func event(for name: String) -> KmpEvent? {
        return events.first { $0.name == name }
    }
    
    func complete(for name: String) {
        events.removeAll { $0.name == name}
        Log("kmp events:\(events.count)")
    }
    
    func registerIMPs() {
        KmpNativeBridge.Companion.shared.registerIos(imp: self)
    }
}


extension KmpBridgeManager: SysKmpNativeBridgeInterface {
    
    func invokeKmpEvent(event: SysKmpToIosEvent) {
        Log("\(event)")

        var responseAction: KmpEventResposeActionProtocol? = nil
        switch event.name {
        case KmpEvent.share:
            responseAction = KmpEventShareResposeAction(eventName: event.name)
        case KmpEvent.colorScheme:
            responseAction = nil
        default:
            assert(false, "no response action for: \(event.name) \(event.inputDatas?.description ?? "")")
        }
        
        let e = KmpEvent(name: event.name, inputDatas: event.inputDatas, outputDatas: event.outputDatas, responseAction: responseAction)
        events.append(e)
        eventPublisher.send(e)
    }
 }
