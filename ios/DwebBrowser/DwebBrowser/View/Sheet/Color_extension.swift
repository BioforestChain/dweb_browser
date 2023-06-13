//
//  Color_extension.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/5/27.
//

import SwiftUI

extension Color {
    
    init(hex8: UInt32) {
        
        let red = CGFloat((hex8 & 0xFF000000) >> 24) / 255.0
        let green = CGFloat((hex8 & 0x00FF0000) >> 16) / 255.0
        let blue = CGFloat((hex8 & 0x0000FF00) >> 8) / 255.0
        let alpha = CGFloat(hex8 & 0x000000FF) / 255.0
        self.init(red: red, green: green, blue: blue, opacity: alpha)
    }
    
    init(hexString: String) {
        let scanner = Scanner(string: hexString)
        scanner.currentIndex = scanner.string.startIndex
        var rgbValue: UInt64 = 0
        scanner.scanHexInt64(&rgbValue)
        
        let r = (rgbValue & 0xff0000) >> 16
        let g = (rgbValue & 0xff00) >> 8
        let b = rgbValue & 0xff
        
        self.init(red: Double(r) / 0xff, green: Double(g) / 0xff, blue: Double(b) / 0xff)
    }
}
