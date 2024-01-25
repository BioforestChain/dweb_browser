//
//  const.swift
//  DwebBrowser
//
//  Created by ui06 on 4/25/23.
//

import Foundation
import SwiftUI
import UIKit

let screen_width = UIScreen.main.bounds.width
let screen_height = UIScreen.main.bounds.height

let addressbarHolder = "搜索或输入网址"

let maxAddressBarH: CGFloat = 60
let minAddressBarH: CGFloat = 25
let maxToolBarH: CGFloat = 50
let minToolBarH: CGFloat = 25

let emptyLink = "about:blank"
let emptyURL = URL(string: emptyLink)!

let gridVSpace: CGFloat = 20.0
let gridHSpace: CGFloat = 18.0

let cellWHratio = 2.0 / 3.0
let cellImageHeightRatio = 0.85

let gridcellCornerR: CGFloat = 10

enum EnterType {
    case search
    case none
}

var enterType: EnterType = .none

var lightSnapshotImage = UIImage.snapshotImage(from: URL.lightSnapshot)
var darkSnapshotImage = UIImage.snapshotImage(from: URL.darkSnapshot)

let webtag = "webtag"
