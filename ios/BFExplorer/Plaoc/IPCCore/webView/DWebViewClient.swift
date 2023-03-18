//
//  DWebViewClient.swift
//  BFExplorer
//
//  Created by ui03 on 2023/3/17.
//

import UIKit
import WebKit

class DWebViewClient: NSObject, WKNavigationDelegate {
   
    private var extends = Extends<WKNavigationDelegate>()
    
    func addWebViewClient(client: WKNavigationDelegate, config: Config = Config()) -> Bool {
        return extends.add(instance: client, config: config)
    }
    
    func removeWebViewClient(client: WKNavigationDelegate) -> Bool {
        return extends.remove(instance: client)
    }
    
    private func inners(methodName: String) -> [WKNavigationDelegate]{
        
        return extends.hasMethod(methodName: methodName)
    }

    func webView(_ webView: WKWebView, decidePolicyFor navigationAction: WKNavigationAction, decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) {
        
        decisionHandler(.allow)
    }
    
    func webView(_ webView: WKWebView, decidePolicyFor navigationResponse: WKNavigationResponse, decisionHandler: @escaping (WKNavigationResponsePolicy) -> Void) {
        
        
        decisionHandler(.allow)
    }
    
    func webView(_ webView: WKWebView, didStartProvisionalNavigation navigation: WKNavigation!) {
        let list = inners(methodName: "webView:didStartProvisionalNavigation:")
        for obj in list {
            obj.webView?(webView, didStartProvisionalNavigation: navigation)
        }
    }
    
    func webView(_ webView: WKWebView, didCommit navigation: WKNavigation!) {
        let list = inners(methodName: "webView:didCommit:")
        for obj in list {
            obj.webView?(webView, didCommit: navigation)
        }
    }
    
    func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
        let list = inners(methodName: "webView:didFinishNavigation:")
        for obj in list {
            obj.webView?(webView, didFinish: navigation)
        }
    }
    
    func webView(_ webView: WKWebView, didFailProvisionalNavigation navigation: WKNavigation!, withError error: Error) {
        let list = inners(methodName: "webView:didFailProvisionalNavigation:withError:")
        for obj in list {
            obj.webView?(webView, didFailProvisionalNavigation: navigation, withError: error)
        }
    }
    
    func webView(_ webView: WKWebView, didFail navigation: WKNavigation!, withError error: Error) {
        let list = inners(methodName: "webView:didFail:withError:")
        for obj in list {
            obj.webView?(webView, didFail: navigation, withError: error)
        }
    }
    
    func webView(_ webView: WKWebView, didReceiveServerRedirectForProvisionalNavigation navigation: WKNavigation!) {
        
    }
    
    func webViewWebContentProcessDidTerminate(_ webView: WKWebView) {
        let list = inners(methodName: "webViewWebContentProcessDidTerminate:")
        for obj in list {
            obj.webViewWebContentProcessDidTerminate?(webView)
        }
    }
    
}


class ReadyHelper: NSObject, WKNavigationDelegate {
   
    
    private var readySignal = SimpleSignal()
    
    func afterReady(cb: @escaping SimpleCallbcak) -> OffListener {
        return readySignal.listen(cb)
    }
    
    func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
        readySignal.emit(())
    }
}
