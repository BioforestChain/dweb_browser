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
let maxToolBarH: CGFloat = 50

let emptyLink = "about:blank"
let emptyURL = URL(string: emptyLink)!

let gridVSpace: CGFloat = 20.0
let gridHSpace: CGFloat = 18.0

let cellWHratio = 2.0 / 3.0
let cellImageHeightRatio = 0.85

let gridcellCornerR: CGFloat = 10

var lightSnapshotImage = UIImage.snapshotImage(from: URL.lightSnapshot)
var darkSnapshotImage = UIImage.snapshotImage(from: URL.darkSnapshot)

let webtag = "webtag"

/**
 * target: The target in which to load the URL, an optional parameter that defaults to _self. (String)
 *  _self: Opens in the Cordova WebView if the URL is in the white list, otherwise it opens in the InAppBrowser.
 *  _blank: Opens in the InAppBrowser.
 *  _system: Opens in the system's web browser.
 */
enum AppBrowserTarget: String {
    case _self = "_self"
    case _blank = "_blank"
    case _system = "_system"
}


//展示选项按钮的最小下拉距离
let minYOffsetToSelectAction: Double = 90

//水平拖拽超过此值时，切换选择
let minXOffsetToChangeAction: Double = 80

