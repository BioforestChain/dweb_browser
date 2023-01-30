//
//  DiskManager.swift
//  Plaoc-iOS
//
//  Created by mac on 2022/8/2.
//

import UIKit

class DiskManager: NSObject {

    //保存数据 数组、字典 字符串
    func preserveData(data:AnyObject, key: AnyObject) {
        
        let filePath = filePath(forKey: key)
        guard filePath.count > 0 else { return }
        let url = URL(fileURLWithPath: filePath)
        if data is Array<Any> || data is [String:Any] {
            let info = try? JSONSerialization.data(withJSONObject: data, options: [])
            try? info?.write(to: url)
        }
        if data is String {
            let dataString = data as! String
            try? dataString.write(to: url, atomically: true, encoding: .utf8)
        }
    }
    
    //查询数据
    func enquiriesData(forKey key: AnyObject) -> AnyObject? {
        let fileManager = FileManager.default
        let filePath = filePath(forKey: key)
        guard filePath.count > 0 else { return nil }
        if fileManager.isReadableFile(atPath: filePath) {
            guard let dataInfo = fileManager.contents(atPath: filePath) else { return nil }
            var content = try? JSONSerialization.jsonObject(with: dataInfo, options: .mutableContainers)
            if content == nil {
                content = String(data: dataInfo, encoding: .utf8)
            }
            return content as AnyObject
        }
        return nil
    }
    //更新数据
    func updataData(data:AnyObject, key: AnyObject) {
        preserveData(data: data, key: key)
        print("更新硬盘中数据")
    }
    //清除缓存数据
    func cleanCacheData(forKey key: AnyObject) {
        
        deleteFolderInCacheFolder(forKey: key)
        print("清除硬盘中数据")
    }
    
    private func filePath(forKey key: AnyObject) -> String {
        guard let key = key as? String else { return "" }
        guard let filePath = NSSearchPathForDirectoriesInDomains(.cachesDirectory, .userDomainMask, true).first else { return "" }
        return filePath + "/" + key
    }
    
    private func createFolderInCacheFolder(forKey key: AnyObject) {
        let fileManager = FileManager.default
        let filePath = filePath(forKey: key)
        guard filePath.count > 0 else { return }
        guard !fileManager.fileExists(atPath: filePath) else { return }
        do {
            try fileManager.createDirectory(atPath: filePath, withIntermediateDirectories: true, attributes: nil)
        } catch {
            NSLog("Couldn't create document directory")
        }
        NSLog("Document directory is \(filePath)")
    }
    //MARK: - Delete selected folder in Cache
    private func deleteFolderInCacheFolder(forKey key: AnyObject) {
        let fileManager = FileManager.default
        let filePath = filePath(forKey: key)
        guard filePath.count > 0 else { return }
        if fileManager.fileExists(atPath: filePath) {
            do {
                try fileManager.removeItem(atPath: filePath)
            } catch {
                NSLog("Couldn't delete directory")
            }
        }
    }
}
