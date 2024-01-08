//
//  Color+Hex.swift
//  DwebBrowser
//
//  Created by ui06 on 6/5/23.
//

import Foundation
import SwiftUI

extension Color{
    static let btnDisabledColor = Color(hex: 0xACB5BF)  // light gray
    static let btnNormalColor = Color(hex: 0x0A1626)  //  dark gray
    static let networkTipColor = Color(hex: 0x737980)  //  network tip color
    static let networkGuidColor = Color(hex: 0xACB5BF)  //
    static let sheetTopbar = Color(assetsColor: "sheetTopBarColor")  //
    static let lightTextColor = btnDisabledColor
    static let AddressbarbkColor = Color(assetsColor: "AddressbarbkColor")
    static let addressTextColor = Color(assetsColor: "addressTextColor")
    static let bkColor = Color(assetsColor: "bkColor")
    static let clearTextColor = Color(assetsColor: "clearTextColor")
    static let dwebTint = Color(assetsColor: "dwebTint")
    static let lineColor = Color(assetsColor: "lineColor")
    static let menubkColor = Color(assetsColor: "menubkColor")
    static let menuTitleColor = Color(assetsColor: "menuTitleColor")
    static let ToolbarColor = Color(assetsColor: "ToolbarColor")
    
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
    
    init(assetsColor: String) {
        self.init(assetsColor, bundle: Bundle.main)
    }
}
