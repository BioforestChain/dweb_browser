//
//  WebViewEvaluator.swift
//  BFExplorer
//
//  Created by ui03 on 2023/3/7.
//

import UIKit
import WebKit
import Combine
import RxSwift

class WebViewEvaluator {

    var webView: WKWebView
    private var idAcc = 0
    private let JS_ASYNC_KIT = "__native_async_callback_kit__"
    private var channelMap: [Int: PassthroughSubject<String,Never>] = [:]
    private let disposeBag = DisposeBag()
    
    init(webView: WKWebView) {
        self.webView = webView
        
        operateMonitor.exposeWkwebViewMonitor.subscribe(onNext: { [weak self] dict in
            guard let strongSelf = self else { return }
            if let type = dict["type"] as? String {
                switch type {
                case "resolve":
                    strongSelf.resolve(dict: dict)
                case "reject":
                    strongSelf.reject(dict: dict)
                default:
                    break
                }
            }
        }).disposed(by: self.disposeBag)
    }
    
    private func initKit() {
        
    }
    
    private func resolve(dict: [String:Any]) {
        
        guard let id = dict["id"] as? Int else { return }
        guard let data = dict["data"] as? String else { return }
        let channel = channelMap.removeValue(forKey: id)
        channel?.send(data)
        channel?.send(completion: .finished)
    }
    
    private func reject(dict: [String:Any]) {
        guard let id = dict["id"] as? Int else { return }
        guard let data = dict["data"] as? String else { return }
        let channel = channelMap.removeValue(forKey: id)
        channel?.send(data)
        channel?.send(completion: .finished)
    }
    
    func evaluateSyncJavascriptCode(script: String) -> String{
        
        let po = PromiseOut<String>()
        webView.evaluateJavaScript(script) { result, error in
            po.resolver(result as? String ?? "")
        }
        return ""//po.waitPromise()
    }
    
    func evaluateAsyncJavascriptCode(script: String, afterEval: @escaping () -> Void) -> String {
        
        
        return ""
    }
}
