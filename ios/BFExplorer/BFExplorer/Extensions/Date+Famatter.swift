//
//  Date+Famatter.swift
//  BFS
//
//  Created by ui06 on 9/13/22.
//

import Foundation

extension Date{
    func detailData() -> String{
        let formatter = DateFormatter()
        let day = "星期" + ["天", "日", "一", "二", "三", "四", "五", "六"][Calendar.current.component(.weekday, from: self)]
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter.string(from: self) + " " + day
    }
    
    func dateMD5()->String{
        longDate().md5
    }
    
    
    //returns "yyyy-MM-dd HH:mm:ss"
    func longDate()->String{
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd HH:mm:ss"
        return formatter.string(from: self)
    }
}
