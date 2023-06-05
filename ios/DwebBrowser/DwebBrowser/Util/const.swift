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
