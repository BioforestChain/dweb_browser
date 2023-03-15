//
//  ViewItem.swift
//  BFExplorer
//
//  Created by ui03 on 2023/3/8.
//

import UIKit

class ViewItem {

    var webviewId: String
    var dWebView: DWebView
    var hidden: Bool = false
    
    init(webviewId: String, dWebView: DWebView, hidden: Bool = false) {
        self.webviewId = webviewId
        self.dWebView = dWebView
        self.hidden = hidden
    }
}
