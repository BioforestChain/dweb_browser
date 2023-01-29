//
//  BatchSystemManager.swift
//  BFS
//
//  Created by ui03 on 2022/9/6.
//

import UIKit

class SystemAppManager: InnerAppManager {

    private var mateDict: [String:Any]?
    
    override var appInstalledPath: String {
        documentdir + "/system-app"
    }
    
    override var filePath: String { documentdir + "/system-app" }

    
    override func iconImagePath(appId: String) -> String {
        return appInstalledPath + "/\(appId)/sys/"
    }
    
    func fetchEntryPath(appId: String) -> String? {
        guard let dict = readBFSAMatedataContent(appId: appId) else { return nil }
        guard let maniDict = dict["manifest"] as? [String:Any] else { return nil }
        guard var entryPath = maniDict["bfsaEntry"] as? String else { return nil }
        entryPath = entryPath.regexReplacePattern(pattern: "^(./|/|../)", replaceString: "")
        let path = appInstalledPath + "/\(appId)/" + entryPath
        return path
    }
    
    //读取matedata.json文件
    func readBFSAMatedataContent(appId: String) -> [String:Any]? {
        let path = appInstalledPath + "/\(appId)/boot/bfsa-metadata.json"
        let manager = FileManager.default
        guard let data = manager.contents(atPath: path) else { return nil }
        guard let content = String(data: data, encoding: .utf8) else { return nil }
        mateDict = ChangeTools.stringValueDic(content)
        return mateDict
    }
    //获取版本号
    func readMetadataVersion(appId: String) -> String? {
        if mateDict == nil {
            _ = readBFSAMatedataContent(appId: appId)
        }
        guard let dict = mateDict?["manifest"] as? [String:Any] else { return nil }
        return dict["version"] as? String
    }
    //获取appType
    func readAppType(appId: String) -> String? {
        if mateDict == nil {
            _ = readBFSAMatedataContent(appId: appId)
        }
        guard let dict = mateDict?["manifest"] as? [String:Any] else { return nil }
        return dict["appType"] as? String
    }
    //获取web类型的网页地址
    func readWebAppURLString(appId: String) -> String? {
        if mateDict == nil {
            _ = readBFSAMatedataContent(appId: appId)
        }
        guard let dict = mateDict?["manifest"] as? [String:Any] else { return nil }
        print(dict)
        return dict["url"] as? String
    }
    
    func isExitSameFile(appId: String) -> Bool {
        let path = appInstalledPath + "/\(appId)"
        if FileManager.default.fileExists(atPath: path) {
            let oldVersion = readMetadataVersion(appId: appId)
            
            return true
        } else {
            return false
        }
    }
}
