//
//  KmpEvent.swift
//  iosApp
//
//  Created by instinct on 2023/11/3.
//  Copyright © 2023 orgName. All rights reserved.
//

import Foundation

extension KmpEvent {
    static let share = "share"
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
