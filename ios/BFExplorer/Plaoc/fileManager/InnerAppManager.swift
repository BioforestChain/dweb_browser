//
//  BatchReadManager.swift
//  BFS
//
//  Created by ui03 on 2022/9/6.
//

import UIKit

class InnerAppManager: NSObject {

    var linkDict: [String : [String : Any]] = [:]
    
    //app的路径：推荐的app（本地配置好的），sys和user（已安装的，本地安装路径）
    var filePath: String {""}
    
    var appInstalledPath: String { return "" }
    
    func iconImagePath(appId: String) -> String { return "" }
    
    //读取各种app的信息（已安装的和推荐安装的）
    func appDirs() -> [String] {
        var appIds: [String] = []
        let filePaths = FileManager.default.subpaths(atPath: filePath)
        guard (filePaths != nil) else { return appIds }
        filePaths?.forEach { appId in
            var isDir: ObjCBool = true
            let fullPath = "\(filePath)/\(appId)"
            if FileManager.default.fileExists(atPath: fullPath, isDirectory: &isDir) {
                if isDir.boolValue {
                    //后续是不需要判断.的，因为这是临时添加的，后续从网络获取
                    if !appId.contains("/"), !appId.contains(".") {
                        appIds.append(appId)
                    }
                }
            }
        }
        return appIds
    }

    //读取bfs-app-id 的link文件信息
    private func readBFSAppLinkContent(appId: String) {
        let path = filePath + "/\(appId)/boot/link.json"
        let manager = FileManager.default
        guard let data = manager.contents(atPath: path) else { return }
        guard let content = String(data: data, encoding: .utf8) else { return }
        let linkConfig = ChangeTools.stringValueDic(content)
        linkDict[appId] = linkConfig
    }
    
    //app名称
    func appName(appId: String) -> String {
        var appConfig = linkDict[appId]
        if appConfig == nil {
            readBFSAppLinkContent(appId: appId)
            appConfig = linkDict[appId]
        }
        return appConfig?["name"] as? String ?? ""
    }
    //app图片
    func appIcon(appId: String) -> UIImage? {
        
        var appConfig = linkDict[appId]
        if appConfig == nil {
            readBFSAppLinkContent(appId: appId)
            appConfig = linkDict[appId]
        }
        
        var imageName = appConfig?["icon"] as? String ?? ""
        if imageName.hasPrefix("file://") {
            imageName = imageName.replacingOccurrences(of: "file://", with: "")
        }
        
        let imagePath = iconImagePath(appId: appId) + imageName
        var image = UIImage(contentsOfFile: imagePath)
        if imagePath.hasSuffix(".svg") {
            image = UIImage.svgImage(withContentsOfFile: imagePath, size: CGSize(width: 28, height: 28))
        }
        return image
    }
    
    func appIconUrlString(appId: String) -> String? {
        var dict = linkDict[appId] as? [String:Any]
        if dict == nil {
            readBFSAppLinkContent(appId: appId)
            dict = linkDict[appId] as? [String:Any]
        }
        
        let imageName = dict?["icon"] as? String
        return imageName
    }
    
    //appID
    func appID(appId: String) -> String {
        var dict = linkDict[appId] as? [String:Any]
        if dict == nil {
            readBFSAppLinkContent(appId: appId)
            dict = linkDict[appId] as? [String:Any]
        }
        return dict?["bfsAppId"] as? String ?? ""
    }
    
    //读取轮询更新缓存信息  取文件夹下面的很多文件
    func readCacheUpdateInfo(appId: String) -> [String:Any]? {
        let filePath = appInstalledPath
        let updatePath = filePath + "/\(appId)/tmp/autoUpdate/"
        let manager = FileManager.default
        let subContents = try? manager.contentsOfDirectory(atPath: updatePath).sorted { $0 > $1 }
        guard let first = subContents?.first else { return nil }
        let path = updatePath + first
        guard let data = manager.contents(atPath: path) else { return nil }
        guard let cacheString = String(data: data, encoding: .utf8) else { return nil }
        return ChangeTools.stringValueDic(cacheString)
    }
    
    
    //写入轮询更新的机制信息
    func writeUpdateInfoToTmpFile(appId: String, json: [String:Any]) {
        let filePath = appInstalledPath
        var updatePath = filePath + "/\(appId)/tmp/autoUpdate/"
        if !FileManager.default.fileExists(atPath: updatePath) {
            try? FileManager.default.createDirectory(atPath: updatePath, withIntermediateDirectories: true)
        }
        var currentTime = Date().dateToString(identifier: "UTC")
        currentTime = currentTime.replacingOccurrences(of: ":", with: "")
        currentTime = currentTime.replacingOccurrences(of: " ", with: "")
        updatePath = updatePath + "\(currentTime).json"
        let jsonString = ChangeTools.dicValueString(json) ?? ""
        guard jsonString.count > 0 else { return }
        if !FileManager.default.fileExists(atPath: updatePath) {
            FileManager.default.createFile(atPath: updatePath, contents: nil)
        }
        
        do {
            try jsonString.write(toFile: updatePath, atomically: true, encoding: .utf8)
        } catch {
            print(error.localizedDescription)
        }
    }
    
    func isNewUpdateInfo(appId: String) -> Bool {
        let filePath = appInstalledPath
        let updatePath = filePath + "/\(appId)/tmp/autoUpdate/"
        let subContents = try? FileManager.default.contentsOfDirectory(atPath: updatePath).sorted { $0 > $1 }
        guard subContents != nil, subContents!.count > 1 else { return true }
        
        let first = subContents![0]
        let second = subContents![1]
        
        let firstPath = updatePath + first
        let secondPath = updatePath + second
        
        let firstVersion = versionInfo(name: firstPath)
        let secondVersion = versionInfo(name: secondPath)
        let result = firstVersion.versionCompare(oldVersion: secondVersion)
        if result == .orderedAscending {
            return true
        }
        return false
    }
                                  
    private func versionInfo(name: String) -> String {
        if let data = FileManager.default.contents(atPath: name) {
            if let cacheString = String(data: data, encoding: .utf8) {
                let oldDict = ChangeTools.stringValueDic(cacheString)
                let version = oldDict?["version"] as? String ?? ""
                return version
//                let versionDouble = Double(version) ?? 0
//                return versionDouble
            }
        }
        return ""
    }
    
    func readAutoUpdateURLInfo(appId: String) -> String? {
        var dict = linkDict[appId] as? [String:Any]
        if dict == nil {
            readBFSAppLinkContent(appId: appId)
            dict = linkDict[appId] as? [String:Any]
        }
        guard let updateDict = dict?["autoUpdate"] as? [String:Any] else { return nil }
        return updateDict["url"] as? String
    }
    
    func readAutoUpdateMaxAge(appId: String) -> Int? {
        var dict = linkDict[appId] as? [String:Any]
        if dict == nil {
            readBFSAppLinkContent(appId: appId)
            dict = linkDict[appId] as? [String:Any]
        }
        guard let updateDict = dict?["autoUpdate"] as? [String:Any] else { return nil }
        return updateDict["maxAge"] as? Int
    }
}
