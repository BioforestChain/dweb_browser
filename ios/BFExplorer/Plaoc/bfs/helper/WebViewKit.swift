//
//  WebViewKit.swift
//  BFExplorer
//
//  Created by ui08 on 2023/2/24.
//

import Foundation
import WebKit

class WebViewAsyncEvalContext {
    let webView: WKWebView
    
    init(webView: WKWebView) {
        self.webView = webView
    }
    
    static private var idAcc = 0
    
    
}
