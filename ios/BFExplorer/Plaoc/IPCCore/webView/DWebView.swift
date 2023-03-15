//
//  DWebView.swift
//  BFExplorer
//
//  Created by ui03 on 2023/3/7.
//

import UIKit
import WebKit

class DWebView: UIView {
    
    var mm: MicroModule?
    var options: Options?
    private var readvPo = PromiseOut<Bool>()
//    private let openSignal = Signal<Message>()
    private let closeSignal = SimpleSignal()
    
    init(frame: CGRect, mm: MicroModule, options: Options) {
        
        super.init(frame: frame)
        self.mm = mm
        self.options = options
        
        self.addSubview(webView)
        
    }
    
    func setUA() -> String{
        
        let baseDwebHost = mm?.mmid
        var dwebHost = baseDwebHost ?? ""
        
        guard let url = URL(string: options?.urlString ?? ""), url.host != nil else { return "" }
        if url.scheme == "http" && url.host!.hasSuffix(".dweb") {
            dwebHost = url.authority()
        }
        if !dwebHost.contains(":") {
            dwebHost += ":80"
        }
        return "dweb-host/\(dwebHost)"
    }
    
    private lazy var webView: WKWebView = {
        
        let config = WKWebViewConfiguration()
        
        config.userContentController = WKUserContentController()
        
        
        config.userContentController.add(LeadScriptHandle(messageHandle: self), name: "resolve")
        config.userContentController.add(LeadScriptHandle(messageHandle: self), name: "reject")
        config.userContentController.add(LeadScriptHandle(messageHandle: self), name: "close")
        config.userContentController.add(LeadScriptHandle(messageHandle: self), name: "close2")
        
        let userAgent = setUA()
        config.applicationNameForUserAgent = userAgent
        
        let prefreen = WKPreferences()
        prefreen.javaScriptCanOpenWindowsAutomatically = true
        config.preferences = prefreen
        config.setValue(true, forKey: "allowUniversalAccessFromFileURLs")
        
        
        let webView = WKWebView(frame: self.bounds, configuration: config)
        webView.navigationDelegate = self
        webView.allowsBackForwardNavigationGestures = true
        if #available(iOS 11.0, *) {
            webView.scrollView.contentInsetAdjustmentBehavior = .never
        } else {

        }
        
        
        return webView
    }()
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func afterReady() -> Bool{
        return readvPo.hasResult()
    }
    
    func resolve(id: Int, data: String) {
        operateMonitor.exposeWkwebViewMonitor.onNext(["type":"resolve","id": id,"data":data])
    }
    
    func reject(id: Int, reason: String) {
        operateMonitor.exposeWkwebViewMonitor.onNext(["type":"reject","id": id,"data":reason])
    }
    
    func evaluateSyncJavascriptCode(script: String) -> String {
        return ""
    }
    
    func evaluateAsyncJavascriptCode(script: String, afterEval: @escaping () -> Void) -> String {
        return ""
    }
    
    func onClose(cb: @escaping SimpleCallbcak) -> OffListener {
        return closeSignal.listen(cb)
    }
    
    func closeWebView() {
        closeSignal.emit(())
    }
    
    deinit {
        closeSignal.emit(())
    }
    
}

extension DWebView:  WKScriptMessageHandler {
    //通过js调取原生操作
    func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
        
        if message.name == "resolve" {
            //TODO 参数
            resolve(id: 0, data: "")
        } else if message.name == "reject" {
            //TODO 参数
            reject(id: 0, reason: "")
        }
    }
    
    private func openAppBoundDomains(urlString: String) -> Bool {
        let domains = ["waterbang.top","plaoc.com"]
        for domain in domains {
            if urlString.contains(domain) {
                return true
            }
        }
        return false
    }
}

extension DWebView: WKNavigationDelegate {
    
    func webView(_ webView: WKWebView, decidePolicyFor navigationAction: WKNavigationAction, decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) {
        
        let request = navigationAction.request
        let host = request.url?.host ?? ""
        if request.httpMethod == "GET" && host.hasSuffix(",dweb") && request.url?.scheme == "http" {
            
            if let response = mm?.nativeFetch(request: request) {
                let header = IpcHeaders(content: response.headers.description)
                let httpResponse = HTTPURLResponse(url: request.url!, statusCode: Int(response.status.code), httpVersion: nil, headerFields: header.headerDict)
                guard let data = response.body.data else { return }
                webView.loadSimulatedRequest(request, response: httpResponse!, responseData: data)
            }
            
            decisionHandler(.cancel)
        } else {
            decisionHandler(.allow)
        }
    }

    func webView(_ webView: WKWebView, decidePolicyFor navigationResponse: WKNavigationResponse, decisionHandler: @escaping (WKNavigationResponsePolicy) -> Void) {
        
        
        decisionHandler(.allow)
    }
    
    func webView(_ webView: WKWebView, didStartProvisionalNavigation navigation: WKNavigation!) {
      
    }
    
    func webView(_ webView: WKWebView, didCommit navigation: WKNavigation!) {
        
    }
    
    func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
        // TODO 这里需要注入脚本，对 fetch、XMLHttpRequest 这些网络请求进行拦截
        readvPo.resolver(true)
    }
    
    func webView(_ webView: WKWebView, didFailProvisionalNavigation navigation: WKNavigation!, withError error: Error) {
        print("didFailProvisionalNavigation")
    }
    
    func webView(_ webView: WKWebView, didFail navigation: WKNavigation!, withError error: Error) {
        print("didFail")
    }
    
    func webView(_ webView: WKWebView, didReceiveServerRedirectForProvisionalNavigation navigation: WKNavigation!) {
        
    }
    
    func webViewWebContentProcessDidTerminate(_ webView: WKWebView) {
        print("webViewWebContentProcessDidTerminate")
    }
}

struct Options {
    
    var urlString: String
}
