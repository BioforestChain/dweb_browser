//
//  Bundle+module.swift
//  DwebWebBrowser
//
//  Created by instinct on 2024/1/2.
//

import Foundation

public extension Bundle {
    static var browser: Bundle {
        let bundle = Bundle(path: Bundle.main.bundlePath + "/Frameworks/DwebWebBrowser.framework")
        return bundle ?? Bundle.main
    }
    
    static var browserResources: Bundle {
        let bundle = Bundle(path: Bundle.main.bundlePath + "/Frameworks/DwebWebBrowser.framework/resource.bundle")
        return bundle ?? Bundle.main
    }
}


