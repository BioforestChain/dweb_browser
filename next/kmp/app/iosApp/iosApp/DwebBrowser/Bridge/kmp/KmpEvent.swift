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

protocol KmpEventResposeActionProtocol {
    var eventName: String { get }
    func doResponseAction()
}

struct KmpEvent {
    let name: String
    let inputDatas: [String: Any]?
    let outputDatas: [String: Any]?
    let responseAction: KmpEventResposeActionProtocol?
}
