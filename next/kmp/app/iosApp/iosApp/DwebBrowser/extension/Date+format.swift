//
//  Date_extension.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/5/6.
//

import SwiftUI

extension Date {
    
    var timeStamp: Int {
        let timeInterval = self.timeIntervalSince1970
        return Int(timeInterval)
    }
    //毫秒
    var milliStamp: Int64 {
        let timeInterval = self.timeIntervalSince1970
        return Int64(round(timeInterval * 1000))
    }
    
    func converDateToString(format: String) -> String {
        
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = format
        return dateFormatter.string(from: self)
    }
    
    //时间戳转Date
    static func timeStampToString(stamp: Int64) -> String {
        
        //时间戳为毫秒级要 ／ 1000， 秒就不用除1000
        let timeStamp = TimeInterval(stamp / 1000)
        let date = Date(timeIntervalSince1970: timeStamp)
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter.string(from: date)
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
    
    //字符串转Date
    static func stringToDate(timeString: String) -> Date? {
        
        let timeZone = TimeZone.init(identifier: "UTC")
        let formatter = DateFormatter()
        formatter.timeZone = timeZone
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter.date(from: timeString)
    }
    
    //根据Date生成显示时间
    static func historyTime(timeString: String) -> String {
        
        guard let date = stringToDate(timeString: timeString) else { return "" }
        let timeString = getYearMonthDay(from: date)
        let weekDay = getWeeday(from: date)

        if timeString == "今天" || timeString == "昨天" {
            return timeString
        }
        return "\(timeString)  \(weekDay)"
    }
    
    //根据Date生成显示时间
    static func getYearMonthDay(from date: Date) -> String {
        let calendar = Calendar.current
        let components = calendar.dateComponents([.year, .month, .day], from: date)
        let currentComponents = calendar.dateComponents([.year, .month, .day], from: Date())
        
        if components.year == currentComponents.year {
            if isToday(date: date) {
                return "今天"
            }
            
            if isYesterday(date: date) {
                return "昨天"
            }
            return "\(handleTimeFormattor(time: components.month))-\(handleTimeFormattor(time: components.day))"
        } else {
            return "\(handleTimeFormattor(time: components.year))-\(handleTimeFormattor(time: components.month))-\(handleTimeFormattor(time: components.day))"
        }
    }
    
    //是否是今天
    static func isToday(date: Date) -> Bool {
        let calendar = Calendar.current
        let components = calendar.dateComponents([.year, .month, .day], from: date)
        let currentComponents = calendar.dateComponents([.year, .month, .day], from: Date())
        return components.year == currentComponents.year && components.month == currentComponents.month && components.day == currentComponents.day
    }
    
    //是否是昨天
    static func isYesterday(date: Date) -> Bool {
        let calendar = Calendar.current
        let components = calendar.dateComponents([.year, .month, .day], from: date)
        let currentComponents = calendar.dateComponents([.year, .month, .day], from: Date())
        let cmps = calendar.dateComponents([.day], from: components, to: currentComponents)
        return cmps.day == 1
    }
    
    //获取星期几
    static func getWeeday(from date: Date) -> String {
        
        let calendar = Calendar.current
        let components = calendar.dateComponents([.weekday], from: date)
        let weekDay = components.weekday
        switch weekDay {
        case 1:
            return "星期天"
        case 2:
            return  "星期一"
        case 3:
            return "星期二"
        case 4:
            return "星期三"
        case 5:
            return "星期四"
        case 6:
            return "星期五"
        case 7:
            return "星期六"
        default:
            return ""
        }
    }
    
    static private func handleTimeFormattor(time: Int?) -> String {
        guard time != nil else { return "00"}
        if time! > 9 {
            return "\(time!)"
        }
        return "0\(time!)"
    }
}
