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
         _ msg: String? = nil) {
#if DEBUG
    print("[\(category)] [\(file.components(separatedBy: "/").last ?? "null"):\(function):\(line)] [\(time)] \(msg ?? "")")
#else
#endif
}

