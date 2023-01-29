//
//  String_extension.swift
//  Plaoc-iOS
//
//  Created by mac on 2022/7/14.
//

import Foundation
import UIKit

extension String {
    //16进制转data
    public func hexData() -> Data? {
        
        var data = Data(capacity: count / 2)
        let regex = try? NSRegularExpression(pattern: "[0-9a-f]{1,2}", options: .caseInsensitive)
        regex?.enumerateMatches(in: self, range: NSMakeRange(0, utf16.count), using: { result, flags, pointer in
            guard result != nil else { return }
            let byteString = (self as NSString).substring(with: result!.range)
            if var num = UInt8(byteString, radix: 16) {
                data.append(&num, count: 1)
            }
        })
        guard data.count > 0 else { return nil }
        return data
    }
    
    public func utf8Data() -> Data? {
        return self.data(using: .utf8)
    }
    
    //16进制转string
    public func hexStringToString(symbol: String) -> String {
        
        guard self.contains(symbol) else { return "" }
        let contentList = self.components(separatedBy: symbol)
        var array: [UInt8] = []
        for text in contentList {
            if UInt8(text) != nil {
                array.append(UInt8(text)!)
            }
        }
        guard array.count > 0 else { return "" }
        let data = Data(bytes: array, count: array.count)
        let result = String(data: data, encoding: .utf8)
        return result ?? ""
    }
    
    //版本号比较
    func versionCompare(oldVersion: String) -> ComparisonResult {
        
        let delimiter = "."
        var currentComponents = self.components(separatedBy: delimiter)
        var oldComponents = oldVersion.components(separatedBy: delimiter)
        
        let diff = currentComponents.count - oldComponents.count
        let zeros = Array(repeating: "0", count: abs(diff))
        if diff > 0 {
            oldComponents.append(contentsOf: zeros)
        } else if diff < 0 {
            currentComponents.append(contentsOf: zeros)
        }
        
        for i in stride(from: 0, to: currentComponents.count, by: 1) {
            let current = currentComponents[i]
            let old = oldComponents[i]
            let currentVersion = Int(current) ?? 0
            let oldVersion = Int(old) ?? 0
            if currentVersion > oldVersion {
                return .orderedAscending
            } else if currentVersion < oldVersion {
                return .orderedDescending
            }
        }
        return .orderedDescending
    }
    
    //正则替换
    func regexReplacePattern(pattern: String, replaceString: String) -> String {
        
        do {
            let regex = try NSRegularExpression(pattern: pattern, options: .caseInsensitive)
            let finalStr = regex.stringByReplacingMatches(in: self, options: NSRegularExpression.MatchingOptions(rawValue: 0), range: NSMakeRange(0, self.count), withTemplate: replaceString)
            return finalStr
        } catch {
            print(error)
        }
        return ""
    }
    //base64转图片
    func base64ToImage() -> UIImage? {
        guard let data = Data(base64Encoded: self, options: .ignoreUnknownCharacters) else { return nil }
        let image = UIImage(data: data)
        return image
    }
    
    //字符串正则匹配
    func match(_ regex: String) -> [[String]] {
        let nsString = self as NSString
        return (try? NSRegularExpression(pattern: regex, options: []))?.matches(in: self, options: [], range: NSMakeRange(0, nsString.length)).map { match in
            (0..<match.numberOfRanges).map { match.range(at: $0).location == NSNotFound ? "" : nsString.substring(with: match.range(at: $0)) }
        } ?? []
    }
}
