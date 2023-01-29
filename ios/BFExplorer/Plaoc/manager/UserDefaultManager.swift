//
//  UserDefaultManager.swift
//  Plaoc-iOS
//
//  Created by ui03 on 2022/12/20.
//

import UIKit

class UserDefaultManager: NSObject {

    static func saveAPPPermission(key: String, value: [String:Any]) {
        UserDefaults.standard.setValue(value, forKey: key)
    }
    
    static func appPermission(key: String) -> [String:Any]? {
        guard let permissionDict = UserDefaults.standard.object(forKey: key) as? [String:Any] else { return nil }
        return permissionDict
    }
    
}
