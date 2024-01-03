//
//  Logger.swift
//  DwebPlatformIosKit
//
//  Created by instinct on 2023/12/4.
//

import Foundation

private var dateformate: DateFormatter = {
    let formatter = DateFormatter()
    formatter.dateFormat = "HH:mm:ss.SSSS"
    return formatter
}()

func Log(time: String = dateformate.string(from: Date()),
         file: String = #file,
         function: String = #function,
         line: Int = #line,
         category: String = "iOS Lib",
         _ msg: String?) {
#if DEBUG
    print("[\(category)] [\(file.components(separatedBy: "/").last ?? "null"):\(function):\(line)] [\(time)] \(msg ?? "")")
#else
#endif
}

