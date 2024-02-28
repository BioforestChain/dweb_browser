//
//  DwebAppRunningMode.swift
//  DwebBrowser
//
//  Created by instinct on 2024/2/28.
//  Copyright © 2024 orgName. All rights reserved.
//

import Foundation

struct DwebAppMode {

    static var isDebug: Bool {
#if DEBUG
        return true
#else
        return false
#endif
    }
    
    static var isRelease: Bool {
        return !isDebug
    }
    
    private static let testModeEnvKey = "DWEB_TEST_MODE"
    private static let testingModeFlag = "DwebTesting"
    private static let uiTestingModeFlag = "DwebUITesting"
    
    static var isTesting: Bool {
        guard let flag = ProcessInfo.processInfo.environment[testModeEnvKey] else {
            return false
        }
        return flag == testingModeFlag
    }
    
    static var isUITesting: Bool {
        guard let flag = ProcessInfo.processInfo.environment[testModeEnvKey] else {
            return false
        }
        return flag == uiTestingModeFlag
    }
    
    static var isNoneTesting: Bool {
        return !isTesting && !isUITesting
    }
}


