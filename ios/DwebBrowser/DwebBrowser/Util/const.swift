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

let toolBarH: CGFloat = 50

let searchTextFieldPlaceholder = "搜索或输入网址"

let addressBarH: CGFloat = 60

let gridcellCornerR: CGFloat = 10
let gridcellBottomH: CGFloat = 35

let emptyLink = "https://http.cat/404"
let emptyURL = URL(string: emptyLink)!
//let emptyURL = URL(string: "")

let gridVSpace: CGFloat = 20.0
let gridHSpace: CGFloat = 18.0

let gridCellW: CGFloat = (screen_width - gridHSpace * 3.0) / 2
let gridCellH: CGFloat = gridCellW * 1.3


var safeAreaTopHeight: CGFloat{
    if let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
       let topSafeAreaInset = windowScene.windows.first?.safeAreaInsets.top {
        return topSafeAreaInset
    }
    return 0
}

var safeAreaBottomHeight: CGFloat{
    if let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
       let topSafeAreaInset = windowScene.windows.first?.safeAreaInsets.bottom {
        return topSafeAreaInset
    }
    return 0
}

let bundlePath = Bundle.main.path(forResource: "resource", ofType: "bundle")!
let bundleUrl = Bundle.main.url(forResource: "resource", withExtension: "bundle")!
