//
//  KmpEvent.swift
//  iosApp
//
//  Created by instinct on 2023/11/3.
//  Copyright © 2023 orgName. All rights reserved.
//

import Foundation

extension KmpEvent {
    /// 唤醒系统share组件事件
    static let share = "share"
    
    /// 主题色切换事件
    static let colorScheme = "colorScheme"
}

class KmpEventResposeAction {
    
    let event: KmpEvent
    
    init(_ event: KmpEvent) {
        self.event = event
    }
    
    func parseInputDatas() -> [Any]? {
        return nil
    }
    
    func doAction() async -> Any? {
        return nil
    }
}

struct KmpEvent {
    let name: String
    let inputDatas: [String: Any]?
}
