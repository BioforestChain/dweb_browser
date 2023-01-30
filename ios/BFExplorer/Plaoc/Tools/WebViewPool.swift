//
//  WebViewPool.swift
//  Plaoc-iOS
//
//  Created by mac on 2022/7/4.
//

import UIKit
import WebKit

protocol WebViewPoolProtocol {
    func webViewWillleavePool()
    func webViewWillEnterPool()
}

class WebViewPool: NSObject {

    static let shared = WebViewPool()
    private var reuseableWebViewSet = Set<XXWebView>()
    private var visiableWebViewSet = Set<XXWebView>()
    private let lock = DispatchSemaphore(value: 1)
    private var capacity: Int = 6
    
    override init() {
        super.init()
        NotificationCenter.default.addObserver(self, selector: #selector(didReceiveMemoryWarning), name: UIApplication.didReceiveMemoryWarningNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(mainInit), name: NSNotification.Name(rawValue: "mainInit"), object: nil)
    }
    
    // 预备一个空的webview
    private func prepareWebView() {
        
        guard reuseableWebViewSet.count <= 0 else { return }
        let webView = XXWebView(frame: .zero, configuration: XXWebView.defaultConfiguration())
        webView.allowsBackForwardNavigationGestures = true
        if #available(iOS 11.0, *) {
            webView.scrollView.contentInsetAdjustmentBehavior = .never
        } else {
            
        }
        reuseableWebViewSet.insert(webView)
    }
    
    // 使用中的webView持有者已销毁，则放回可复用池中
    func tryCompactWeakHolders() {
        lock.wait()
        let reusedset = visiableWebViewSet.filter{ $0.holderObj == nil }
        for webView in reusedset {
            webView.webViewWillEnterPool()
            visiableWebViewSet.remove(webView)
            reuseableWebViewSet.insert(webView)
        }
        lock.signal()
    }
    
    func getReuseWebViewForHolder() -> WKWebView {
        return reuseableWebViewSet.removeFirst()
    }
}

extension WebViewPool {
    
    @objc private func didReceiveMemoryWarning() {
        lock.wait()
        reuseableWebViewSet.removeAll()
        lock.signal()
    }
    
    @objc private func mainInit() {
        DispatchQueue.main.asyncAfter(deadline: DispatchTime.now() + 0.25) {
            self.prepareWebView()
        }
    }
}

extension WebViewPool {
    // 获取可复用的webView
    func getReusedWebView(forHolder holder: AnyObject?) -> XXWebView {
        
        guard let holder = holder else {
            return XXWebView(frame: .zero, configuration: XXWebView.defaultConfiguration())
        }
        
        tryCompactWeakHolders()
        lock.wait()
        let webView: XXWebView
        if reuseableWebViewSet.count > 0 {
            webView = reuseableWebViewSet.randomElement()!
            reuseableWebViewSet.remove(webView)
            visiableWebViewSet.insert(webView)
            webView.webViewWillleavePool()
        } else {
            webView = XXWebView(frame: .zero, configuration: XXWebView.defaultConfiguration())
            visiableWebViewSet.insert(webView)
        }
        
        webView.holderObj = holder
        lock.signal()
        return webView
    }
    
    // 回收可复用的webView到复用池中
    func recycleReusedWebView(webView: XXWebView?) {
        guard webView != nil else { return }
        lock.wait()
        if visiableWebViewSet.contains(webView!) {
            webView?.webViewWillEnterPool()
            visiableWebViewSet.remove(webView!)
            reuseableWebViewSet.insert(webView!)
        }
        lock.signal()
    }
    
    // 移除并销毁所有复用池的webView
    func clearAllReusableWebViews() {
        lock.wait()
        for webView in reuseableWebViewSet {
            webView.webViewWillEnterPool()
        }
        reuseableWebViewSet.removeAll()
        lock.signal()
    }
}
