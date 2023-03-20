//
//  MultiWebViewControllerManager.swift
//  BFExplorer
//
//  Created by ui03 on 2023/3/18.
//

import UIKit

class MultiWebViewControllerManager {

    typealias Callback = (String) -> Any?
    
    private var webviewId_acc = 1
    private var mmid: String = ""
    var localeMM: MicroModule?
    var remoteMM: MicroModule?
    private var webViewList: [ViewItem] = []
    
    private let webViewCloseSignal = Signal<String>()
    private let webViewOpenSignal = Signal<String>()
    
    private var activityTask = PromiseOut<MutilWebViewViewController>()
    
    var activity: MutilWebViewViewController? = nil {
        didSet {
            if activity == nil {
                activityTask = PromiseOut<MutilWebViewViewController>()
            } else {
                activityTask.resolver(activity!)
            }
        }
    }
    
    init(mmid: String, localeMM: MicroModule, remoteMM: MicroModule) {
        self.mmid = mmid
        self.localeMM = localeMM
        self.remoteMM = remoteMM
    }
    
    func waitActivityCreated() {
        activityTask.waitPromise()
    }
    
    func openWebView(url: String) -> ViewItem {
        return appendWebViewAsItem(dWebView: createDwebView(url: url)!)
    }
    
    func createDwebView(url: String) -> DWebView? {
        
        guard let app = UIApplication.shared.delegate as? AppDelegate else { return nil }
        
        let currentActivity = activity ??  app.window?.rootViewController
        
        let dWebView = DWebView(frame: .zero, localeMM: localeMM!, remoteMM: remoteMM!, options: Options(urlString: url, onDetachedFromWindowStrategy: .Ignore))
        return dWebView
    }
    
    func appendWebViewAsItem(dWebView: DWebView) -> ViewItem {
        
        let webviewId = "#w\(webviewId_acc)"
        webviewId_acc += 1
        //TODO
        return ViewItem(webviewId: "", dWebView: dWebView)
    }
    
    func closeWebView(webviewId: String) -> Bool {
        
        for i in stride(from: 0, to: webViewList.count, by: 1) {
            let viewItem = webViewList[i]
            if viewItem.webviewId == webviewId {
                if self.webViewList.count == 1 {
                    if let url = URL(string: "file://dns.sys.dweb/close")?.addURLQuery(name: "app_id", value: mmid) {
                        self.localeMM?.nativeFetch(url: url)
                    }
                }
                webViewList.remove(at: i)
                viewItem.webView.destory()
                webViewCloseSignal.emit(webviewId)
                return true
            }
        }
        return false
    }
    
    func moveToTopWebView(webviewId: String) -> Bool {
        if let index = webViewList.firstIndex(where: { $0.webviewId == webviewId }) {
            webViewList.remove(at: index)
            webViewList.append(webViewList[index])
            return true
        }
        return false
    }
    
    func conWebViewClose(cb: @escaping Callback) -> OffListener {
        return webViewCloseSignal.listen(cb)
    }
    
    func onWebViewOpen(cb: @escaping Callback)  -> OffListener {
        return webViewOpenSignal.listen(cb)
    }
}
