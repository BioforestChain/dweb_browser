//
//  HPKWebViewPool.swift
//  Plaoc-iOS
//
//  Created by mac on 2022/7/5.
//

import UIKit
import WebKit

class HPKWebViewPool: NSObject {

    static let shared = HPKWebViewPool()
    private var dequeueWebViews: [String: WKWebView] = [:]
    private var enqueueWebViews: [String: WKWebView] = [:]
    private let lock = DispatchSemaphore(value: 1)
    
    override init() {
        super.init()
    }
    
    // 获取可复用的WKWebView
    func dequeueWebViewWithClass(webView:  WKWebView, holder: AnyObject) {
        
    }
    
    //回收可复用的WKWebView
    func enqueueWebView(webView: WKWebView) {
        
    }
    
    //回收并销毁WKWebView，并且将之从回收池里删除
    func removeReusableWebView(webView: WKWebView) {
        
    }
    
    //销毁全部在回收池中的WebView
    func clearAllReusableWebViews() {
        
    }
    
    //销毁在回收池中特定Class的WebView
    func clearAllReusableWebView(webView: WKWebView) {
        
    }
    
    // 重新刷新在回收池中的WebView
    func reloadAllReusableWebViews() {
        
    }
    
    
    private func getWebViewWithClass(webView: AnyClass) {
        
    }
}
