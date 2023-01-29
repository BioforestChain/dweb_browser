//
//  BatchTempManager.swift
//  Plaoc-iOS
//
//  Created by ui03 on 2022/12/21.
//

import UIKit

class BatchTempManager: NSObject {

    private var mateDict: [String:Any]?
    func tempFilePath(name: String) -> String {
        return NSTemporaryDirectory() + name
    }
    
    func readTempMetadata(name: String) {
        let path = tempFilePath(name: name) + "/boot/bfsa-metadata.json"
        let manager = FileManager.default
        guard let data = manager.contents(atPath: path) else { return }
        guard let content = String(data: data, encoding: .utf8) else { return }
        let dict = ChangeTools.stringValueDic(content)
        mateDict = dict?["manifest"] as? [String:Any]
    }
    
    func tempAppVersion(name: String) -> String {
        if mateDict == nil {
            readTempMetadata(name: name)
        }
        return mateDict?["version"] as? String ?? ""
    }
    
    func tempAppType(name: String) -> String {
        if mateDict == nil {
            readTempMetadata(name: name)
        }
        return mateDict?["appType"] as? String ?? ""
    }
    
    func tempAppURL(name: String) -> String {
        if mateDict == nil {
            readTempMetadata(name: name)
        }
        return mateDict?["url"] as? String ?? ""
    }
    
    func tempAppName(name: String) -> String {
        if mateDict == nil {
            readTempMetadata(name: name)
        }
        return mateDict?["name"] as? String ?? ""
    }
    
    func tempAppIcon(name: String) -> String {
        if mateDict == nil {
            readTempMetadata(name: name)
        }
        return mateDict?["icon"] as? String ?? ""
    }
}
