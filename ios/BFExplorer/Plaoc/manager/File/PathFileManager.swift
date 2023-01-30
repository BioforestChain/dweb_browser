//
//  PathFileManager.swift
//  Plaoc-iOS
//
//  Created by ui03 on 2022/10/20.
//

import UIKit

class PathFileManager: NSObject {

    func list(appId: String, path: String) -> [FileModel] {
"""
        "/home/user/haha/test.txt"
        name = "/home/user/haha/test.txt"
        extname = "txt"
        path = "/home/user/haha/test.txt"
        cwd = "/home/user/haha"
        type = "?"
        isLink = false
        relativePath = "/home/user/haha"
"""
        
        let filePath = rootFilePath(fileName: appId) + path
        let manager = FileManager.default
        guard manager.fileExists(atPath: filePath) else { return [] }
        var currentFile = createCurrentFileModel(path: filePath)
        let subFiles = try? manager.contentsOfDirectory(atPath: filePath)
        if subFiles == nil { //文件
            currentFile.type = "file"
            return [currentFile]
        } else { //文件夹
            currentFile.type = "directory"
            var subModels = createSubFileModels(path: path)
            subModels.insert(currentFile, at: 0)
            return subModels
        }
    }
    
    //得到当前的文件对象
    private func createCurrentFileModel(path: String) -> FileModel {
        var fileModel = FileModel()
        fileModel.name = path
        fileModel.extname = URL(fileURLWithPath: path).pathExtension
        fileModel.path = path
        fileModel.cwd = URL(fileURLWithPath: path).deletingLastPathComponent().absoluteString
        fileModel.isLink = false
        fileModel.relativePath = URL(fileURLWithPath: path).relativeString
        return fileModel
    }
    
    //得到当前文件夹及其子文件路径地址
    private func fetchFilePaths(path: String, isRecursive: Bool = false, filter: [String:Any]?) -> [String]? {
        var pathLists: [String] = []
        let manager = FileManager.default
        guard let paths = try? manager.contentsOfDirectory(atPath: path) else { return [] }
        if filter != nil {
            //TODO 过滤一些条件路径
            /**
             let attri = try? manager.attributesOfItem(atPath: sub)
             print(attri![FileAttributeKey(rawValue: FileAttributeKey.type.rawValue)])  //文件类型: NSFileTypeRegular 文件  NSFileTypeDirectory 文件夹
             */
        }
        for subPath in paths {
            pathLists.append(path + "/" + subPath)
        }
        guard isRecursive else { return pathLists }
        guard let recursivePaths = try? manager.enumerator(atPath: path)?.allObjects as? [String] else { return [] }
        if filter != nil {
            //TODO 过滤一些条件路径
        }
        pathLists.removeAll()
        for subPath in recursivePaths {
            pathLists.append(path + "/" + subPath)
        }
        return pathLists
    }
    
    //得到当前文件夹及其子文件对象，递归遍历
    private func createSubFileModels(path: String) -> [FileModel] {
        let manager = FileManager.default
        guard let enumeratorPaths = try? manager.contentsOfDirectory(atPath: path) else { return [] }
        var fileArray: [FileModel] = []
        for subPath in enumeratorPaths {
            let totalPath = path + "/" + subPath
            var fileModel = FileModel()
            fileModel.name = totalPath
            fileModel.extname = URL(fileURLWithPath: totalPath).pathExtension
            fileModel.path = totalPath
            fileModel.cwd = URL(fileURLWithPath: totalPath).deletingLastPathComponent().absoluteString
            fileModel.isLink = false
            fileModel.relativePath = URL(fileURLWithPath: totalPath).relativeString
            let subFiles = try? manager.contentsOfDirectory(atPath: totalPath)
            if subFiles == nil { //文件
                fileModel.type = "file"
            } else { //文件夹
                fileModel.type = "directory"
            }
            fileArray.append(fileModel)
        }
        return fileArray
    }
    
    //创建文件夹
    func mkdir(fileName: String, path: String) -> Bool {
        let filePath = rootFilePath(fileName: fileName) + path
        do {
            try FileManager.default.createDirectory(atPath: filePath, withIntermediateDirectories: true, attributes: nil)
        } catch {
            print(error.localizedDescription)
            return false
        }
        return true
    }
    //读取数据返回二进制
    func readFileData(fileName: String, path: String) -> Data? {
        let filePath = rootFilePath(fileName: fileName) + path
        guard FileManager.default.fileExists(atPath: filePath) else { return nil }
        let stream = InputStream(fileAtPath: filePath)
        stream?.open()
        defer {
            stream?.close()
        }
        
        let bufferSize = 1024
        let buffer = UnsafeMutablePointer<UInt8>.allocate(capacity: bufferSize)
        defer {
            buffer.deallocate()
        }
        var resultData = Data()
        while stream!.hasBytesAvailable {
            let length = stream!.read(buffer, maxLength: bufferSize)
            let data = Data(bytes: buffer, count: length)
            resultData.append(data)
        }
        return resultData
    }
    
    //读取数据返回字符串
    func readFileContent(fileName: String, path: String) -> String {
        let filePath = rootFilePath(fileName: fileName) + path
        guard FileManager.default.fileExists(atPath: filePath) else { return "" }
        let stream = InputStream(fileAtPath: filePath)
        stream?.open()
        defer {
            stream?.close()
        }

        let bufferSize = 1024
        let buffer = UnsafeMutablePointer<UInt8>.allocate(capacity: bufferSize)
        defer {
            buffer.deallocate()
        }
        var result: String = ""
        while stream!.hasBytesAvailable {
            let length = stream!.read(buffer, maxLength: bufferSize)
            let data = Data(bytes: buffer, count: length)
            let content = String(data: data, encoding: .utf8) ?? ""
            result += content
        }
        return result
    }
    //写数据进入文件
    func writeFileContent(fileName: String, path: String, content: String?,append: Bool = false, autoCreate: Bool = true) {
        
        guard content != nil, let contentData = content!.data(using: .utf8) else { return }
        let filePath = rootFilePath(fileName: fileName) + path
        
        if !append { //如果覆盖写入，先清空文件
            if FileManager.default.fileExists(atPath: filePath) {
                try? "".write(toFile: filePath, atomically: true, encoding: .utf8)
            }
        }
        if !FileManager.default.fileExists(atPath: filePath) {
            if autoCreate { //如果文件不存在，则创建
                let cwdPath = URL(fileURLWithPath: filePath).deletingLastPathComponent().absoluteString
                try? FileManager.default.createDirectory(atPath: cwdPath, withIntermediateDirectories: true, attributes: nil)
                FileManager.default.createFile(atPath: filePath, contents: nil)
            } else {
                return
            }
        }
        
        let output = OutputStream(url: URL(fileURLWithPath: filePath), append: append)
        output?.open()
        defer {
            output?.close()
        }
        //下面二种方式都可以写入
        output!.write(content!, maxLength: contentData.count)
//        _ = contentData.withUnsafeBytes {
//                output?.write($0.bindMemory(to: UInt8.self).baseAddress!, maxLength: contentData.count)
//        }
    }
    
    func removeFile(fileName: String, path: String) {
        let filePath = rootFilePath(fileName: fileName) + path
        guard FileManager.default.fileExists(atPath: filePath) else { return }
        try? FileManager.default.removeItem(atPath: filePath)
    }
    
    
    private func rootFilePath(fileName: String) -> String {
        return documentdir + "/system-app" + "/\(fileName)/home/"
    }
}
