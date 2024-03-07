//
//  DwebAppRunningMode.swift
//  DwebBrowser
//
//  Created by instinct on 2024/2/28.
//  Copyright © 2024 orgName. All rights reserved.
//

import Foundation

@objcMembers public class DwebConfigInfo: NSObject {
    
    public static var isDebug: Bool {
#if DEBUG
        return true
#else
        return false
#endif
    }
    
    public static var isRelease: Bool {
        return !isDebug
    }
    
    private static let testModeEnvKey = "Mode"
    private static let orderOutputEnvKey = "Order"
    private static let gitKey = "Git"

    
    private static let testingModeFlag = "DwebTesting"
    private static let uiTestingModeFlag = "DwebUITesting"
    private static let orderDumpModeFlag = "DwebOrderDumping"

    public static var isTesting: Bool {
        guard let flag = getValue(testModeEnvKey) else {
            return false
        }
        return flag == testingModeFlag
    }
    
    public static var isUITesting: Bool {
        guard let flag = getValue(testModeEnvKey) else {
            return false
        }
        return flag == uiTestingModeFlag
    }
    
    public static var isNoneTesting: Bool {
        return !isTesting && !isUITesting
    }
    
    public static var isOrderDump: Bool {
        guard let flag = getValue(testModeEnvKey) else {
            return false
        }
        return flag == orderDumpModeFlag
    }
    
    static func getValue(_ key: String) -> String? {
        guard let v = info?[key] as? String, var index = v.firstIndex(of: ":") else {
            return nil
        }
        index = v.index(after: index)
        return String(v[index...])
    }
    
    public static var orderOutputPath: String? {
        return getValue(orderOutputEnvKey)
    }
    
    public static var gitInfo: String? {
        return getValue(gitKey)
    }
    
    public static var info: [String: String]? {
        guard let path = Bundle.main.path(forResource: "Info", ofType: "plist"),
              let dic = NSDictionary(contentsOf: URL(filePath: path)) as? [String: Any],
              let info = dic["DwebConfigs"] as? [String: String] else { return nil }
        return info
    }
}
