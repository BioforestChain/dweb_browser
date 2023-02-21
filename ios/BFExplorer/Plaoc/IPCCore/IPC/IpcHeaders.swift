//
//  IpcHeaders.swift
//  IPC
//
//  Created by ui03 on 2023/2/13.
//

import UIKit
import CryptoKit

class IpcHeaders {

    private(set) var headerDict: [String: String] = [:]
    
    init() {
        
    }
    
    init(key: String, value: String) {
        headerDict[key] = value
    }
    
    init(content: String) {
        
        guard let array = stringValueArray(content) else { return }
        for (key, value) in array {
            headerDict[key] = value
        }
    }
    
    func set(key: String, value: String) {
        headerDict[key] = value
    }
    
    func getValue(forKey key: String) -> String? {
        return headerDict[key]
    }
    
    func has(key: String) -> Bool {
        return headerDict.keys.contains(key)
    }
    
    func deleteValue(forKey key: String) {
        headerDict.removeValue(forKey: key)
    }
    
    private func stringValueArray(_ str: String) -> [(String, String)]? {
        let data = str.data(using: String.Encoding.utf8)
        if let array = try? JSONSerialization.jsonObject(with: data!,
                        options: .mutableContainers) as? [(String, String)] {
            return array
        }

        return nil
    }
}
