//
//  Date_extension.swift
//  BFS
//
//  Created by ui03 on 2022/8/26.
//

import Foundation

extension Date {
    
    var timeStamp: Int {
        let timeInterval = self.timeIntervalSince1970
        return Int(timeInterval)
    }
    
    var milliStamp: Int {
        let timeInterval = self.timeIntervalSince1970
        return Int(round(timeInterval * 1000))
    }
    //UTC时间
    func dateToString(identifier: String? = nil) -> String {
        let format = DateFormatter()
        if identifier != nil {
            format.timeZone = TimeZone(identifier: identifier!)
        }
        format.dateFormat = "YYYY:MM:dd HH:mm:ss"
        let dateString = format.string(from: self)
        return dateString
    }
}
