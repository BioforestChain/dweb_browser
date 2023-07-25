//
//  Color+Hex.swift
//  DwebBrowser
//
//  Created by ui06 on 6/5/23.
//

import Foundation
import SwiftUI

extension Color{
    static let dwebTint = Color(hex: 0x3DD4F2)  // light blue
    static let btnDisabledColor = Color(hex: 0xACB5BF)  // light gray
    static let btnNormalColor = Color(hex: 0x0A1626)  //  dark gray
    static let bkColor = Color(hex: 0xF5F6F7)  //  background color light gray
    static let networkTipColor = Color(hex: 0x737980)  //  network tip color
    static let networkGuidColor = Color(hex: 0xACB5BF)  //
    static let sheetTopbar = Color(hex: 0xD8D8D8)  //  
    static let lightTextColor = btnDisabledColor
    
    init(hex: UInt32) {
        let red = Double((hex >> 16) & 0xff) / 255.0
        let green = Double((hex >> 8) & 0xff) / 255.0
        let blue = Double(hex & 0xff) / 255.0
        self.init(red: red, green: green, blue: blue)
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
