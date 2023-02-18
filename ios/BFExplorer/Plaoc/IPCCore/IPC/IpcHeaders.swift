//
//  IpcHeaders.swift
//  IPC
//
//  Created by ui03 on 2023/2/13.
//

import UIKit

class IpcHeaders {

    private var headerDict: [String: String] = [:]
    
    init(key: String, value: String) {
        headerDict[key] = value
    }
    
    func set(key: String, value: String) {
        headerDict[key] = value
    }
    
    func getValue(forKey key: String) -> String {
        return headerDict[key] ?? ""
    }
    
    
}
