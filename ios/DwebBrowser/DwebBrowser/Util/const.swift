//
//  const.swift
//  DwebBrowser
//
//  Created by ui06 on 4/25/23.
//

import UIKit
import Foundation
import SwiftUI

let screen_width = UIScreen.main.bounds.width
let screen_height = UIScreen.main.bounds.height

let toolBarHeight: CGFloat = 50

let addressBarH: CGFloat = 60

let gridcellCornerR: CGFloat = 10
let gridcellBottomH: CGFloat = 35

let testLink = "https://www.hackingwithswift.com/quick-start"
let testURL = URL(string: testLink)!

let gridVSpace: CGFloat = 20.0
let gridHSpace: CGFloat = 18.0

let gridCellW: CGFloat = (screen_width - gridHSpace * 3.0) / 2
let gridCellH: CGFloat = gridCellW * 1.3

extension Color{
    static let dwebTint = Color(hex: 0x3DD4F2)  // light blue
    static let btnDisabledColor = Color(hex: 0xACB5BF)  // light gray
    static let btnNormalColor = Color(hex: 0x0A1626)  //  dark gray

    init(hex: UInt32) {
        let red = Double((hex >> 16) & 0xff) / 255.0
        let green = Double((hex >> 8) & 0xff) / 255.0
        let blue = Double(hex & 0xff) / 255.0
        self.init(red: red, green: green, blue: blue)
    }
}
