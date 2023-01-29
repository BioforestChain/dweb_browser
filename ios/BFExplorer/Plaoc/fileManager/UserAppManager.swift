//
//  BatchScanManager.swift
//  Plaoc-iOS
//
//  Created by ui03 on 2022/10/18.
//

import UIKit

class UserAppManager: InnerAppManager {
    override var appInstalledPath: String {
        documentdir + "/user-app"
    }
    
    
    func writeLinkJson(appId: String, dict: [String:Any]) {
        
        let jsonString = ChangeTools.dicValueString(dict) ?? ""
        guard jsonString.count > 0 else { return }
        let path = filePath + "/\(appId)/boot/"
        if !FileManager.default.fileExists(atPath: path) {
            try? FileManager.default.createDirectory(atPath: path, withIntermediateDirectories: true)
        }
        let linkPath = path + "link.json"
        if !FileManager.default.fileExists(atPath: linkPath) {
            FileManager.default.createFile(atPath: linkPath, contents: nil)
        }
        do {
            try jsonString.write(toFile: linkPath, atomically: true, encoding: .utf8)
        } catch {
            print(error.localizedDescription)
        }
    }
}
