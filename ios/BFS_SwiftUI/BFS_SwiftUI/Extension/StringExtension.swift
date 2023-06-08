//
//  StringExtension.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/4/20.
//

import Foundation

extension String {
    /**
     Convert argb string to rgba string.
     */
    var argb2rgba: String? {
        guard self.hasPrefix("#") else {
            return nil
        }
        
        let hexString: String = String(self[self.index(self.startIndex, offsetBy: 1)...])
        switch hexString.count {
        case 4:
            return "#\(String(hexString[self.index(self.startIndex, offsetBy: 1)...]))\(String(hexString[..<self.index(self.startIndex, offsetBy: 1)]))"
        case 8:
            return "#\(String(hexString[self.index(self.startIndex, offsetBy: 2)...]))\(String(hexString[..<self.index(self.startIndex, offsetBy: 2)]))"
        default:
            return nil
        }
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
}
