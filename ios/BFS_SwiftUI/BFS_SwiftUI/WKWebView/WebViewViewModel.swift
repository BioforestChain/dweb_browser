//
//  WebViewViewModel.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/4/24.
//

import SwiftUI
import WebKit

final class WebViewViewModel: NSObject, ObservableObject {
    
    var webView: WKWebView!
    
    private var progressToken: NSKeyValueObservation?
    
    private var backToken: NSKeyValueObservation?
    
    private var forwardToken: NSKeyValueObservation?
    
    init(jsNames: [String]? = nil, messageHandles: [String]? = nil, replyHandles: [String]? = nil) {
        super.init()
        
        let config = WKWebViewConfiguration()
        config.userContentController = WKUserContentController()
        addUserScript(userContentController: config.userContentController, jsNames: jsNames)
        addMessageHandles(userContentController: config.userContentController, messageHandles: messageHandles)
        addMessageReplyHandles(userContentController: config.userContentController, replyHandles: replyHandles)
        
        let prefreen = WKPreferences()
        prefreen.javaScriptCanOpenWindowsAutomatically = true
        config.preferences = prefreen
        
        webView = WKWebView(frame: .zero, configuration: config)
        webView.navigationDelegate = self
        webView.uiDelegate = self
        webView.allowsBackForwardNavigationGestures = true
        webView.scrollView.keyboardDismissMode = .onDrag
        if #available(iOS 11.0, *) {
            webView.scrollView.contentInsetAdjustmentBehavior = .never
        } else {

        }
    }
}

extension WebViewViewModel {
    
    func loadUrl(urlString: String) {
        
        guard let url = URL(string: urlString) else { return }
        webView.load(URLRequest(url: url))
    }
    
    func cancelLodUrl() {
        webView.stopLoading()
    }
    
    func addWebViewObserver() {
        
        progressToken = webView.observe(\.estimatedProgress, options: .new) { view, change in
            let progress = Float(change.newValue ?? 0)
            NotificationCenter.default.post(name: NSNotification.Name.progress, object: nil, userInfo: ["progress":progress])
        }
        
        backToken = webView.observe(\.canGoBack, options: .new) { view, change in
            NotificationCenter.default.post(name: NSNotification.Name.goBack, object: nil, userInfo: ["goBack":change.newValue ?? false])
        }
        
        forwardToken = webView.observe(\.canGoForward, options: .new) { view, change in
            NotificationCenter.default.post(name: NSNotification.Name.goForward, object: nil, userInfo: ["goForward":change.newValue ?? false])
        }
    }
    
    func removeWebViewObserver() {
        progressToken?.invalidate()
        backToken?.invalidate()
        forwardToken?.invalidate()
        NotificationCenter.default.removeObserver(self)
    }
    
    func goBack() {
        if webView.canGoBack {
            webView.goBack()
        }
    }
    
    func goForward() {
        if webView.canGoForward {
            webView.goForward()
        }
    }
    
    //保存到数据库
    private func saveToHistoryCoreData(urlString: String, title: String) {
        let incognitoMode = UserDefaults.standard.bool(forKey: "incognitoMode")
        guard !incognitoMode else { return }
        let manager = HistoryCoreDataManager()
        let link = LinkRecord(link: urlString, imageName: "", title: title, createdDate: Date().milliStamp)
        manager.insertHistory(history: link)
    }
    
}

extension WebViewViewModel {
    
    //注入脚本
    private func addUserScript(userContentController: WKUserContentController, jsNames: [String]?) {
        guard let jsNames = jsNames,jsNames.count > 0 else { return }
        for jsName in jsNames {
          let path = Bundle.main.bundlePath + "/JSScript/" +  jsName + ".js"  //js脚本统一放到JSScript文件夹中
            let url = URL(fileURLWithPath: path)
            let data = try? Data(contentsOf: url)
            if data != nil {
                if let jsString = String(data: data!, encoding: .utf8) {
                    let script = WKUserScript(source: jsString, injectionTime: .atDocumentStart, forMainFrameOnly: true)
                    userContentController.addUserScript(script)
                }
            }
        }
    }
    
    //添加js交互
    private func addMessageHandles(userContentController: WKUserContentController, messageHandles: [String]?) {
        guard let handles = messageHandles, handles.count > 0 else { return }
        for name in handles {
            userContentController.add(LeadScriptHandle(messageHandle: self), name: name)
        }
    }
    
    //添加js交互,并返回数据给js
    private func addMessageReplyHandles(userContentController: WKUserContentController, replyHandles: [String]?) {
        guard let handles = replyHandles, handles.count > 0 else { return }
        for name in handles {
            userContentController.addScriptMessageHandler(LeadReplyHandle(messageHandle: self), contentWorld: .page, name: name)
        }
    }
}

//js 和原生交互
extension WebViewViewModel:  WKScriptMessageHandler {
    
    func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
        
    }
}

//js 和原生交互 并返回结果给js
extension WebViewViewModel:  WKScriptMessageHandlerWithReply {
    
    func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage, replyHandler: @escaping (Any?, String?) -> Void) {
        
    }

}


extension WebViewViewModel: WKNavigationDelegate {
    
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
        let urlString = webView.url?.absoluteString ?? ""
        let title = webView.title ?? ""
        guard urlString.count > 0, title.count > 0 else { return }
        NotificationCenter.default.post(name: NSNotification.Name.webViewTitle, object: nil, userInfo: ["title": title, "urlString": urlString])
        
        saveToHistoryCoreData(urlString: urlString, title: title)
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


extension WebViewViewModel: WKUIDelegate {
    /*
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
    }*/
}
