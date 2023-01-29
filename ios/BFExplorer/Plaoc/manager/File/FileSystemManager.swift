//
//  FileSystemManager.swift
//  Plaoc-iOS
//
//  Created by ui03 on 2022/12/19.
//

import UIKit

class FileSystemManager: NSObject {

    public enum FilesystemError: LocalizedError {
        case noParentFolder, noSave, failEncode, noAppend, notEmpty
        
        public var errorDescription: String? {
            switch self {
            case .noParentFolder:
                return "Parent folder doesn't exist"
            case .noSave:
                return "Unable to save file"
            case .failEncode:
                return "Unable to encode data to utf-8"
            case .noAppend:
                return "Unable to append file"
            case .notEmpty:
                return "Folder is not empty"
            }
        }
    }
    
    static func readFile(at fileUrl: URL, with encoding: Bool = false) throws -> String {
        if encoding {
            let data = try String(contentsOf: fileUrl, encoding: .utf8)
            return data
        } else {
            let data = try Data(contentsOf: fileUrl)
            return data.base64EncodedString()
        }
    }
    
    static func writeFile(at fileUrl: URL, with data: String, recursive: Bool, encoding: Bool = false) throws -> String {
        
        if !FileManager.default.fileExists(atPath: fileUrl.deletingLastPathComponent().path) {
            if recursive {
                try FileManager.default.createDirectory(at: fileUrl.deletingLastPathComponent(), withIntermediateDirectories: recursive)
            } else {
                throw FilesystemError.noParentFolder
            }
        }
        if encoding {
            try data.write(to: fileUrl, atomically: false, encoding: .utf8)
        } else {
            if let base64Data = Data(base64Encoded: data) {
                try base64Data.write(to: fileUrl)
            } else {
                throw FilesystemError.noSave
            }
        }
        return fileUrl.absoluteString
    }
    
    static func appendFile(at fileUrl: URL, with data: String, recursive: Bool, with encoding: Bool = false) throws {
        if FileManager.default.fileExists(atPath: fileUrl.path) {
            let fileHandle = try FileHandle.init(forWritingTo: fileUrl)
            var writeData: Data?
            if encoding {
                guard let userData = data.data(using: .utf8) else { throw FilesystemError.failEncode }
                writeData = userData
            } else {
                if let base64Data = Data(base64Encoded: data) {
                    writeData = base64Data
                } else {
                    throw FilesystemError.noAppend
                }
            }
            defer {
                fileHandle.closeFile()
            }
            fileHandle.seekToEndOfFile()
            fileHandle.write(writeData!)
        } else {
            _ = try writeFile(at: fileUrl, with: data, recursive: recursive, encoding: encoding)
        }
    }
    
    static func deleteFile(at fileUrl: URL) throws {
        if FileManager.default.fileExists(atPath: fileUrl.path) {
            try FileManager.default.removeItem(atPath: fileUrl.path)
        }
    }
    
    static func mkdir(at fileUrl: URL, recursive: Bool) throws {
        try FileManager.default.createDirectory(at: fileUrl, withIntermediateDirectories: recursive)
    }
    
    static func rmdir(at fileUrl: URL, recursive: Bool) throws {
        print(fileUrl)
        print(recursive)
        let directoryContents = try FileManager.default.contentsOfDirectory(at: fileUrl, includingPropertiesForKeys: nil, options: [])
        if directoryContents.count != 0 && !recursive {
            throw FilesystemError.notEmpty
        }
        try FileManager.default.removeItem(at: fileUrl)
    }
    
    static func readdir(at fileUrl: URL) throws -> [URL] {
        return try FileManager.default.contentsOfDirectory(at: fileUrl, includingPropertiesForKeys: nil, options: [])
    }
    
    static func stat(at fileUrl: URL) throws -> [FileAttributeKey:Any] {
        return try FileManager.default.attributesOfItem(atPath: fileUrl.path)
    }
    
    static func getType(from attr: [FileAttributeKey:Any]) -> String {
        let fileType = attr[.type] as? String ?? ""
        if fileType == "NSFileTypeDirectory" {
            return "directory"
        } else {
            return "file"
        }
    }
    
    static func rename(at srcURL: URL, to dstURL: URL) throws {
        try privateCopy(at: srcURL, to: dstURL, doRename: true)
    }
    
    static func copy(at srcURL: URL, to dstURL: URL) throws {
        try privateCopy(at: srcURL, to: dstURL, doRename: false)
    }
    
    static private func privateCopy(at srcURL: URL, to dstURL: URL, doRename: Bool) throws {
        if srcURL == dstURL {
            return
        }
        var isDir: ObjCBool = false
        if FileManager.default.fileExists(atPath: dstURL.path, isDirectory: &isDir) {
            if !isDir.boolValue {
                try? FileManager.default.removeItem(at: dstURL)
            }
        }
        if doRename {
            try FileManager.default.moveItem(at: srcURL, to: dstURL)
        } else {
            try FileManager.default.copyItem(at: srcURL, to: dstURL)
        }
    }
    
    static func getDirectory(directory: String?) -> FileManager.SearchPathDirectory? {
     
        guard directory != nil else { return nil }
        switch directory! {
        case "CACHE":
            return .cachesDirectory
        case "LIBRARY":
            return .libraryDirectory
        default:
            return .documentDirectory
        }
    }
    
    static func getFileUrl(at path: String, in directory: String?) -> URL? {
        if let directory = getDirectory(directory: directory) {
            guard let dir = FileManager.default.urls(for: directory, in: .userDomainMask).first else { return nil }
            if !path.isEmpty {
                return dir.appendingPathComponent(path)
            }
            return dir
        } else {
            return URL(string: path)
        }
    }
}
