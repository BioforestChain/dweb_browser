//
//  DWebView.swift
//  BFExplorer
//
//  Created by ui03 on 2023/3/7.
//

import UIKit
import WebKit
import Vapor

class DWebView: WKWebView {
    
    var localeMM: MicroModule?
    var remoteMM: MicroModule?
    var options: Options?
    private var readvPo = PromiseOut<Bool>()
    //    private let openSignal = Signal<Message>()
    private let closeSignal = SimpleSignal()
    
    var filePathCallback: FilePathProtocol?
    var requestPermissionCallback: RequestPermissionCallback?
    
    private var readyHelper: ReadyHelper?
    
    private var readyHelperLock = NSLock()
    
    private let internalWebClient = InternalWebViewClient.shared
    
    private var evaluator: WebViewEvaluator!
    
    init(frame: CGRect, localeMM: MicroModule, remoteMM: MicroModule, options: Options) {
        
        self.localeMM = localeMM
        self.remoteMM = remoteMM
        self.options = options
        
        let config = DWebView.setUA(remoteMM: remoteMM, options: options)
        super.init(frame: frame, configuration: config)
        
        self.scrollView.contentInsetAdjustmentBehavior = .never
        self.allowsBackForwardNavigationGestures = true
        self.navigationDelegate = self
        _ = dWebViewClient.addWebViewClient(client: self)
        
        internalWebClient.remoteMM = remoteMM
        internalWebClient.localeMM = localeMM
        
        evaluator = WebViewEvaluator(webView: self)
        
        if let url = URL(string: options.urlString) {
            load(URLRequest(url: url))
        }
    }
    
    static func setUA(remoteMM: MicroModule, options: Options) -> WKWebViewConfiguration {
        
        let config = WKWebViewConfiguration()
        
        let baseDwebHost = remoteMM.mmid
        var dwebHost = baseDwebHost
        
        guard let url = URL(string: options.urlString), url.host != nil else { return config }
        if url.scheme == "http" && url.host!.hasSuffix(".dweb") {
            dwebHost = url.authority()
        }
        if !dwebHost.contains(":") {
            dwebHost += ":80"
        }
        
        config.applicationNameForUserAgent = "dweb-host/\(dwebHost)"
        
        let preference = WKPreferences()
        preference.javaScriptCanOpenWindowsAutomatically = true
        config.preferences = preference
        
        return config
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    lazy var dWebViewClient: DWebViewClient = {
        let client = DWebViewClient()
        _ = client.addWebViewClient(client: self.internalWebClient)
        return client
    }()
    
    func onReady(cb: @escaping SimpleCallbcak) {
        
        if self.readyHelper == nil {
            readyHelperLock.withLock {
                self.readyHelper = ReadyHelper()
                _ = self.dWebViewClient.addWebViewClient(client: self.readyHelper!)
                _ = self.readyHelper?.afterReady(cb: { _ in
                    print("ready")
                })
            }
        }
        _ = self.readyHelper?.afterReady(cb: cb)
    }
    
    private func afterReady() -> Bool{
        return readvPo.hasResult()
    }
    
    func evaluateSyncJavascriptCode(script: String) -> String {
        return evaluator.evaluateSyncJavascriptCode(script: script)
    }
    
    func evaluateAsyncJavascriptCode(script: String, afterEval: @escaping () -> Void) async -> String {
        return await evaluator.evaluateAsyncJavascriptCode(script: script, afterEval: afterEval)
    }
    
    func onClose(cb: @escaping SimpleCallbcak) -> OffListener {
        return closeSignal.listen(cb)
    }
    
    func closeWebView() {
        closeSignal.emit(())
    }
    
    func destory() {
        
    }
    
    deinit {
        closeSignal.emit(())
    }
    
}

extension DWebView: WKNavigationDelegate {
    
    func webView(_ webView: WKWebView, decidePolicyFor navigationAction: WKNavigationAction, decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) {
        
        let request = navigationAction.request
        let host = request.url?.host ?? ""
        if request.httpMethod == "GET" && host.hasSuffix(",dweb") && request.url?.scheme == "http" {
            
//            if let response = mm?.nativeFetch(request: request) {
//                let header = IpcHeaders(content: response.headers.description)
//                let httpResponse = HTTPURLResponse(url: request.url!, statusCode: Int(response.status.code), httpVersion: nil, headerFields: header.headerDict)
//                guard let data = response.body.data else { return }
//                webView.loadSimulatedRequest(request, response: httpResponse!, responseData: data)
//            }
            
            decisionHandler(.allow)
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
        closeSignal.emit(())
    }
}

struct Options {
    
    /**
     * 要加载的页面
     */
    var urlString: String
    /**
     * WebChromeClient.onJsBeforeUnload 的策略
     *
     * 用户可以额外地进行策略补充
     */
    var onJsBeforeUnloadStrategy = JsBeforeUnloadStrategy.Default
    /**
     * WebView.onDetachedFromWindow 的策略
     *
     * 如果修改了它，就务必注意 WebView 的销毁需要自己去管控
     */
    var onDetachedFromWindowStrategy = DetachedFromWindowStrategy.Default
    
    init(urlString: String, onJsBeforeUnloadStrategy: JsBeforeUnloadStrategy = .Default, onDetachedFromWindowStrategy: DetachedFromWindowStrategy = .Default) {
        self.urlString = urlString
        self.onJsBeforeUnloadStrategy = onJsBeforeUnloadStrategy
        self.onDetachedFromWindowStrategy = onDetachedFromWindowStrategy
    }
}


enum JsBeforeUnloadStrategy {
    case Default
    case Cancel
    case Confirm
}

enum DetachedFromWindowStrategy {
    case Default
    case Ignore
}


class InternalWebViewClient: NSObject, WKNavigationDelegate {
    
    static let shared = InternalWebViewClient()
    
    var remoteMM: MicroModule?
    var localeMM: MicroModule?
    
    override init() {
        super.init()
        
    }
    
    func webView(_ webView: WKWebView, decidePolicyFor navigationAction: WKNavigationAction, decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) {
        
        let request = navigationAction.request
        let host = request.url?.host ?? ""
        if request.httpMethod == "GET" && host.hasSuffix(",dweb") && (request.url?.scheme == "http"  || request.url?.scheme == "https") {
            
            let req = Request.new(url: request.url?.absoluteString ?? "")
            var headers = HTTPHeaders()
            for (key,value) in request.allHTTPHeaderFields ?? [:] {
                headers.add(name: key, value: value)
            }
            headers.add(name: "X-Dweb-Proxy-Id", value: localeMM?.mmid ?? "")
            req.headers = headers
            
            if let response = remoteMM?.nativeFetch(request: req) {
                let header = IpcHeaders(content: response.headers.description)
                let httpResponse = HTTPURLResponse(url: request.url!, statusCode: Int(response.status.code), httpVersion: "HTTP/1.1", headerFields: header.headerDict)
                guard let data = response.body.data else { return }
                webView.loadSimulatedRequest(request, response: httpResponse!, responseData: data)
            }
            decisionHandler(.cancel)
        } else {
            decisionHandler(.allow)
        }
    }
}
