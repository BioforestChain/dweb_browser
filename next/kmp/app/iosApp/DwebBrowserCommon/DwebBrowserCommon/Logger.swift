//
//  Logger.swift
//  DwebPlatformIosKit
//
//  Created by instinct on 2023/12/4.
//

import Foundation

public var dateformate: DateFormatter = {
    let formatter = DateFormatter()
    formatter.dateFormat = "HH:mm:ss.SSSS"
    return formatter
}()

public func Log(time: String = dateformate.string(from: Date()),
                file: String = #file,
                function: String = #function,
                line: Int = #line,
                category: String = "iOS",
                _ msg: String? = nil,
                separator: String = " ",
                terminator: String = "\n")
{
    #if DEBUG
        print("[\(category)] [\(file.components(separatedBy: "/").last ?? "null"):\(function):\(line)] [\(time)] \(msg ?? "")", separator: separator, terminator: terminator)
    #else
    #endif
}

public func LogRaw(_ msg: String? = nil,
                   separator: String = " ",
                   terminator: String = "\n")
{
    #if DEBUG
        print("\(msg ?? "")", separator: separator, terminator: terminator)
    #else
    #endif
}

public func Log(time: String = dateformate.string(from: Date()),
                file: String = #file,
                function: String = #function,
                line: Int = #line,
                category: String = "iOS",
                _ msg: () -> String?)
{
    #if DEBUG
        Log(time: time, file: file, line: line, category: category, msg())
    #else
    #endif
}
