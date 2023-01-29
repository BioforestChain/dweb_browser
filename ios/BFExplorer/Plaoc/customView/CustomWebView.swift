//
//  CustomWebView.swift
//  WebViewController
//
//  Created by mac on 2022/3/15.
//

import UIKit
import WebKit
import SwiftyJSON

typealias UpdateTitleCallback = (String) -> Void

enum KeyboardType {
    case show
    case hidden
}

class CustomWebView: UIView {

    var jsManager: JSCoreManager?
    var callback: UpdateTitleCallback?
    var superVC: UIViewController?
    private var scripts: [WKUserScript]?
    private let imageUrl_key: String = "imageUrl"
    private var isKeyboardOverlay: Bool = false
    private var keyHeight:CGFloat = 0
    private var appId: String = ""
    private var loadingUrlString: String = ""
    
    init(frame: CGRect, jsNames: [String], appId: String, urlString: String) {
        super.init(frame: frame)
        scripts = addUserScript(jsNames: jsNames)
        self.appId = appId
        self.loadingUrlString = urlString
        self.addSubview(webView)
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    func removeUserScripts() {
        webView.configuration.userContentController.removeAllUserScripts()
        if #available(iOS 14.0, *) {
            webView.configuration.userContentController.removeAllScriptMessageHandlers()
        } else {
            // Fallback on earlier versions
        }
    }
    
    private lazy var webView: WKWebView = {
        
        let config = WKWebViewConfiguration()
        
        config.userContentController = WKUserContentController()
        if self.scripts != nil {
            for script in self.scripts! {
                config.userContentController.addUserScript(script)
            }
        }
        //true 可以下载  
        config.userContentController.add(LeadScriptHandle(messageHandle: self), name: "InstallBFS")
        config.userContentController.add(LeadScriptHandle(messageHandle: self), name: "getConnectChannel")
        config.userContentController.add(LeadScriptHandle(messageHandle: self), name: "postConnectChannel")
        config.userContentController.add(LeadScriptHandle(messageHandle: self), name: "logging")
        let prefreen = WKPreferences()
        prefreen.javaScriptCanOpenWindowsAutomatically = true
        config.preferences = prefreen
        config.setValue(true, forKey: "allowUniversalAccessFromFileURLs")
        config.setURLSchemeHandler(Schemehandler(appId: self.appId), forURLScheme: schemeString)
        
        
        let webView = WKWebView(frame: self.bounds, configuration: config)
        webView.navigationDelegate = self
        webView.uiDelegate = self
        webView.allowsBackForwardNavigationGestures = true
        if #available(iOS 11.0, *) {
            webView.scrollView.contentInsetAdjustmentBehavior = .never
        } else {

        }
        
//        let webView = WebViewPool.shared.getReusedWebView(forHolder: self)
//        webView.frame = self.bounds
//        webView.uiDelegate = self
//        webView.navigationDelegate = self
//        addScriptMessageHandler(config: webView.configuration)
//        addScriptMessageHandlerWithReply(config: webView.configuration)
        return webView
    }()
    
}

extension CustomWebView {
    //注入脚本
    private func addUserScript(jsNames: [String]) -> [WKUserScript]? {
        guard jsNames.count > 0 else { return nil }
        var scripts: [WKUserScript] = []
        for jsName in jsNames {
          let path = Bundle.main.bundlePath + "/app/injectWebView/" +  jsName + ".js"
            let url = URL(fileURLWithPath: path)
            let data = try? Data(contentsOf: url)
            if data != nil {
                if let jsString = String(data: data!, encoding: .utf8) {
                    let script = WKUserScript(source: jsString, injectionTime: .atDocumentStart, forMainFrameOnly: true)
                    scripts.append(script)
                }
            }
        }
        return scripts.count > 0 ? scripts : nil
    }
    
    func openWebView(html: String) {
        var html_url = html + "\(html.contains("?") ? "&" : "?")v=\(Date().timeIntervalSince1970)"
        if let url = URL(string: html_url) {
            let request = URLRequest(url: url)
//            let request = URLRequest(url: url, cachePolicy: .reloadIgnoringLocalAndRemoteCacheData, timeoutInterval: 30)
            self.webView.load(request)
        }
    }
    
    func openLocalWebView(name: String) {

        var path = name
        path = "file://".appending(path)
        if let url = URL(string: path) {
            self.webView.load(URLRequest(url: url, cachePolicy: .reloadIgnoringLocalAndRemoteCacheData, timeoutInterval: 30))
        }
    }
    
    func handleJavascriptString(inputJS: String) {
        webView.evaluateJavaScript(inputJS) { (response, error) in
            print( response , error)
        }
    }

    func closeWebViewBackForwardNavigationGestures(isClose: Bool) {
        webView.allowsBackForwardNavigationGestures = !isClose
    }
    
    func canGoback() -> Bool {
        return webView.canGoBack
    }
    
    func goBack() {
        if webView.canGoBack {
            webView.goBack()
        }
    }
    
    func recycleWebView() {
//        WebViewPool.shared.recycleReusedWebView(webView: webView)
    }
    
    func updateFrame(frame: CGRect) {
        webView.frame = CGRect(origin: .zero, size: frame.size)
    }
}

extension CustomWebView:  WKScriptMessageHandler {
    //通过js调取原生操作
    func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
        
        if message.name == "InstallBFS" {
            print(message.body)
            //点击网页按钮 开始加载
            guard let bodyDict = message.body as? [String:String] else { return }
            guard let path = bodyDict["path"] else { return }
            BFSNetworkManager.shared.downloadApp(urlString: path)
            //同时显示下载进度条
        } else if message.name == "logging" {
            print(message.body)
        } else if message.name == "getConnectChannel" {
            print("swift#getConnectChannel",message.body)
            guard let bodyDict = message.body as? [String:String] else { return }
            guard let param = bodyDict["param"] else { return }
            jsManager?.handleEvaluateScript(jsString: param)
        } else if message.name == "postConnectChannel" {
            print("swift#postConnectChannel", message.body)
            guard let bodyDict = message.body as? [String:Any] else { return }
            guard let strPath = bodyDict["strPath"] as? String else { return }
            guard let cmd = bodyDict["cmd"] as? String else { return }
            guard let buffer = bodyDict["buffer"] as? String else { return }
            jsManager?.handleEvaluatePostScript(strPath: strPath, cmd: cmd, buffer: buffer)
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

extension CustomWebView: UIDocumentPickerDelegate {
    
    func documentPicker(_ controller: UIDocumentPickerViewController, didPickDocumentsAt urls: [URL]) {
        controller.dismiss(animated: true)
        print(urls)
    }
    
    func documentPickerWasCancelled(_ controller: UIDocumentPickerViewController) {
        controller.dismiss(animated: true)
    }
}

extension CustomWebView: WKNavigationDelegate {
    
    func webView(_ webView: WKWebView, decidePolicyFor navigationAction: WKNavigationAction, decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) {
        
        decisionHandler(.allow)
    }

    func webView(_ webView: WKWebView, decidePolicyFor navigationResponse: WKNavigationResponse, decisionHandler: @escaping (WKNavigationResponsePolicy) -> Void) {
        
        decisionHandler(.allow)
    }
    
    func webView(_ webView: WKWebView, didStartProvisionalNavigation navigation: WKNavigation!) {
      
    }
    
    func webView(_ webView: WKWebView, didCommit navigation: WKNavigation!) {
        
    }
    
    func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
        print("didFinish")
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


extension CustomWebView: WKUIDelegate {
    
    func webView(_ webView: WKWebView, runJavaScriptAlertPanelWithMessage message: String, initiatedByFrame frame: WKFrameInfo, completionHandler: @escaping () -> Void) {
        let controller = self.currentViewController()
        let alert = UIAlertController(title: "TIPS", message: message, preferredStyle: .alert)
        let sureAction = UIAlertAction(title: "OK", style: .default) { action in
            completionHandler()
        }
        alert.addAction(sureAction)
        controller.present(alert, animated: true)
        
    }
    func webView(_ webView: WKWebView, runJavaScriptConfirmPanelWithMessage message: String, initiatedByFrame frame: WKFrameInfo, completionHandler: @escaping (Bool) -> Void){
        let controller = self.currentViewController()
        let alert = UIAlertController(title: "TIPS", message: message, preferredStyle: .alert)
        let sureAction = UIAlertAction(title: "OK", style: .default) { action in
            completionHandler(true)
        }
        alert.addAction(sureAction)
        controller.present(alert, animated: true)
        
    }
    func webView(_ webView: WKWebView, runJavaScriptTextInputPanelWithPrompt prompt: String, defaultText: String?, initiatedByFrame frame: WKFrameInfo, completionHandler: @escaping (String?) -> Void){
        let controller = self.currentViewController()
        let alert = UIAlertController(title: "TIPS", message: defaultText, preferredStyle: .alert)
        let sureAction = UIAlertAction(title: "OK", style: .default) { action in
            completionHandler(alert.textFields?.first?.text ?? "")
        }
        let sureAction2 = UIAlertAction(title: "CANCEL", style: .default) { action in
            completionHandler(alert.textFields?.first?.text ?? "")
        }
        alert.addAction(sureAction)
        alert.addAction(sureAction2)
        alert.addTextField { textField in
            textField.text = prompt
            textField.placeholder = defaultText
        }
        controller.present(alert, animated: true)
    }
    
    func webView(_ webView: WKWebView, createWebViewWith configuration: WKWebViewConfiguration, for navigationAction: WKNavigationAction, windowFeatures: WKWindowFeatures) -> WKWebView? {
        let ismain = navigationAction.targetFrame?.isMainFrame ?? true
        print(ismain)
        if ismain {
            let wk = WKWebView(frame: webView.frame, configuration: configuration)
            wk.uiDelegate = self
            wk.navigationDelegate = self
            wk.load(navigationAction.request)
            
            let vc = UIViewController()
            vc.modalPresentationStyle = .fullScreen
            vc.view = wk
            
            let controller = currentViewController()
            controller.navigationController?.pushViewController(vc, animated: true)
            return wk
            
        }
        return nil
    }
}
