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

let toolBarH: CGFloat = 50

let addressbarHolder = "搜索或输入网址"

let addressBarH: CGFloat = 60



let emptyLink = "https://http.cat/404"
let emptyURL = URL(string: emptyLink)!

let gridVSpace: CGFloat = 20.0
let gridHSpace: CGFloat = 18.0

let gridCellW: CGFloat = (screen_width - gridHSpace * 3.0) / 2
let gridCellH: CGFloat = gridCellW * 1.5

let cellImageH: CGFloat = gridCellH * 0.9
let gridcellBottomH: CGFloat = gridCellH * 0.1
let gridcellCornerR: CGFloat = 10

//let cellImageH: CGFloat = gridCellW * 1.3

private var curSafeAreaInsets: UIEdgeInsets {
    if let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
       let insets = windowScene.windows.first?.safeAreaInsets
    {
        return insets
    }
    return .zero
}

var safeAreaTopHeight: CGFloat {
    return curSafeAreaInsets.top
}

var safeAreaBottomHeight: CGFloat {
    return curSafeAreaInsets.bottom
}
