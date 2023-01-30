//
//  FileEntites.swift
//  BFExplorer
//
//  Created by ui03 on 2022/12/23.
//

import UIKit
import SwiftyJSON

let documentdir = NSSearchPathForDirectoriesInDomains(.documentDirectory, .userDomainMask, true).first!

extension PlaocHandleModel {

    // 获取dweb app home目录路径
    func getDwebAppPath() -> String {
        documentdir + "/system-app/\(appId)/home"
    }
    
    func pathPrefixReplace(_ path: String) -> String {
        return path.regexReplacePattern(pattern: "^[.]+", replaceString: "")
    }
    	
    // 获取指定文件系统目录下的内容
    func executiveFileSystemLs(param: Any) -> String {
        guard let param = param as? String else { return "" }
        let data = JSON.init(parseJSON: param)
        let homePath = getDwebAppPath()

        do {
            let urls = try FileSystemManager.readdir(at: URL(fileURLWithPath: homePath + pathPrefixReplace(data["path"].stringValue)))
            
            if urls.count > 0 {
                
                let urlPaths = try urls.filter { url in
                    if data["option"]["filter"].exists() {
                        var result = false
                        try data["option"]["filter"].arrayValue.forEach { filterItem in
                            var typeBool = false
                            var nameBool = false
                            
                            // 用于判断文件类型是否符合
                            if filterItem["type"].exists() {
                                let fileAttr = try FileSystemManager.stat(at: url)
                                let fileType = FileSystemManager.getType(from: fileAttr)

                                if fileType == filterItem["type"].stringValue {
                                    typeBool = true
                                }
                            } else {
                                typeBool = true
                            }

                            // 用于判断文件名是否符合
                            if filterItem["name"].exists() {
                                let urlName = url.lastPathComponent
                                let nameArr = filterItem["name"].arrayValue.map{ $0.stringValue }

                                nameArr.forEach { name in
                                    // *开头的正则表达式会报错，是用.*替代，转义.
                                    let regex = name.replacingOccurrences(of: ".", with: "\\.").replacingOccurrences(of: "*", with: ".*")
                                    
                                    // String扩展了match方法，用于正则匹配
                                    if urlName.match(regex).count > 0 {
                                        nameBool = true
                                    }
                                }
                            } else {
                                nameBool = true
                            }

                            if typeBool && nameBool {
                                result = true
                            }
                        }

                        return result
                    } else {
                        return true
                    }
                }.map { $0.path.replacingOccurrences(of: homePath, with: "") }
                
                let str = ChangeTools.arrayValueString(urlPaths)
                
                return str ?? "[]"
            } else {
                return "[]"
            }
        } catch {
            print("ls error: \(error)")
            return "\(error)"
        }
    }
    
    // 在指定文件系统目录下创建目录
    func executiveFileSystemMkdir(param: Any) -> Bool {
        guard let param = param as? String else { return false }
        let data = JSON.init(parseJSON: param)
        let homePath = getDwebAppPath()
        
        do {
            try FileSystemManager.mkdir(at: URL(fileURLWithPath: homePath + pathPrefixReplace(data["path"].stringValue)), recursive: data["option"]["recursive"].boolValue)
            
            return true
        } catch {
            print("mkdir error: \(error)")
            return false
        }
    }
    
    // 删除指定文件系统某个目录或文件
    func executiveFileSystemRm(param: Any) -> Bool {
        guard let param = param as? String else { return false }
        let data = JSON.init(parseJSON: param)
        let homePath = getDwebAppPath()
        
        do {
            let url = URL(fileURLWithPath: homePath + pathPrefixReplace(data["path"].stringValue))
            // 获取文件类型
            let fileAttr = try FileSystemManager.stat(at: url)
            let fileType = FileSystemManager.getType(from: fileAttr)
            
            if fileType == "file" {
                try FileSystemManager.deleteFile(at: url)
            } else {
                var recursive = true
                
                if data["option"]["deepDelete"].exists() {
                    recursive = data["option"]["deepDelete"].boolValue
                }
                
                try FileSystemManager.rmdir(at: url, recursive: recursive)
            }
            
            return true
        } catch {
            print("rm error: \(error)")
            return false
        }
    }
    
    // 读取指定文件系统内容
    func executiveFileSystemRead(param: Any) -> String {
        guard let param = param as? String else { return "" }
        let data = JSON.init(parseJSON: param)
        let homePath = getDwebAppPath()
        
        do {
            let result = try FileSystemManager.readFile(at: URL(fileURLWithPath: homePath + pathPrefixReplace(data["path"].stringValue)), with: true)
            
            return result
        } catch {
            print("read error: \(error)")
            return "\(error)"
        }
    }
    
    // 在指定文件系统下写入内容
    func executiveFileSystemWrite(param: Any) -> Bool {
        guard let param = param as? String else { return false }
        let data = JSON.init(parseJSON: param)
        let homePath = getDwebAppPath()
        
        do {
            let url = URL(fileURLWithPath: homePath + pathPrefixReplace(data["path"].stringValue))
            if data["option"]["append"].boolValue {
                try FileSystemManager.appendFile(at: url, with: data["content"].stringValue, recursive: data["option"]["autoCreate"].boolValue, with: true)
            } else {
                try FileSystemManager.writeFile(at: url, with: data["content"].stringValue, recursive: data["option"]["autoCreate"].boolValue, encoding: true)
            }
            
            return true
        } catch {
            print("write error: \(error)")
            return false
        }
    }
    
    // 获取指定文件详细信息
    func executiveFileSystemStat(param: Any) -> String {
        guard let param = param as? String else { return "" }
        let data = JSON.init(parseJSON: param)
        let homePath = getDwebAppPath()
        
        do {
            let url = URL(fileURLWithPath: homePath + pathPrefixReplace(data["path"].stringValue))
            // 获取文件类型
            let fileAttr = try FileSystemManager.stat(at: url)
            let fileType = FileSystemManager.getType(from: fileAttr)
            
            var dict: [String:Any] = [:]
            
            dict["type"] = fileType
            dict["size"] = fileAttr[.size]
            dict["uri"] = url.path.replacingOccurrences(of: homePath, with: "")
            dict["mtime"] = (fileAttr[.modificationDate] as? Date)?.description
            dict["ctime"] = (fileAttr[.creationDate] as? Date)?.description

            let str = ChangeTools.dicValueString(dict)
            
            return str ?? ""
        } catch {
            print("stat error: \(error)")
            return "\(error)"
        }
    }
    
    // 获取指定文件系统目录信息
    func executiveFileSystemList(param: Any) -> String {
        guard let param = param as? String else { return "" }
        let data = JSON.init(parseJSON: param)
        let homePath = getDwebAppPath()
        
        do {
            let url = URL(fileURLWithPath: homePath + pathPrefixReplace(data["path"].stringValue))
            let result = StreamFileManager().list(appId: appId, filePath: url.path)
            let jsonEncoder = JSONEncoder()
            let jsonData = try jsonEncoder.encode(result)
            let jsonString = String(data: jsonData, encoding: .utf8)
            
            return jsonString ?? ""
        } catch {
            print("list error: \(error)")
            return "\(error)"
        }
    }
    
    // 重命名文件
    func executiveFileSystemRename(param: Any) -> Bool {
        guard let param = param as? String else {
            return false
        }
        
        let data = JSON.init(parseJSON: param)
        let homePath = getDwebAppPath()
        
        do {
            try FileSystemManager.rename(at: URL(fileURLWithPath: homePath + pathPrefixReplace(data["path"].stringValue)), to: URL(fileURLWithPath: homePath + pathPrefixReplace(data["newPath"].stringValue)))
            
            return true
        } catch {
            print("rename error: \(error)")
            return false
        }
    }
    
    // 以buffer的形式读取文件
    func executiveFileSystemReadBuffer(param: Any) -> [UInt8]? {
        guard let param = param as? String else {
            return nil
        }
        
        let data = JSON.init(parseJSON: param)
        let homePath = getDwebAppPath()
        let url = URL(fileURLWithPath: homePath + pathPrefixReplace(data["path"].stringValue))
        let result = StreamFileManager().readFileData(filePath: url.path)
        
        if result != nil {
            return [UInt8](result!)
        } else {
            return nil
        }
    }
}
