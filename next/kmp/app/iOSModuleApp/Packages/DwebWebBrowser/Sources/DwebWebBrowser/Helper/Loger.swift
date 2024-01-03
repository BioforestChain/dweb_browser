//
//  Loger.swift
//  iosApp
//
//  Created by instinct on 2023/11/7.
//  Copyright © 2023 orgName. All rights reserved.
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
         category: String = "iOS",
         _ msg: String?) {
#if DEBUG
    print("[\(category)] [\(file.components(separatedBy: "/").last ?? "null"):\(function):\(line)] [\(time)] \(msg ?? "")")
#else
#endif
}
