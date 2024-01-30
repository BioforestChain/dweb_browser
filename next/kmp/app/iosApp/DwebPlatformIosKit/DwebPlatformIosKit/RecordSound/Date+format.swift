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
}
