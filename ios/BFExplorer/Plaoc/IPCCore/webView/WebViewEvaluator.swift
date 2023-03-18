//
//  WebViewEvaluator.swift
//  BFExplorer
//
//  Created by ui03 on 2023/3/7.
//

import UIKit
import WebKit
import Combine

class WebViewEvaluator: NSObject {

    var webView: WKWebView!
    private var idAcc = 0
    private let JS_ASYNC_KIT = "__native_async_callback_kit__"
    private var channelMap: [Int: PassthroughSubject<String,MyError>] = [:]
    private var cancellable: AnyCancellable?
    
    init(webView: WKWebView) {
        super.init()
        self.webView = webView
        
        initKit()
      
    }
    
    private func initKit() {
        
        webView.configuration.userContentController.add(LeadScriptHandle(messageHandle: self), name: "resolve")
        webView.configuration.userContentController.add(LeadScriptHandle(messageHandle: self), name: "reject")
    }
    
    func evaluateSyncJavascriptCode(script: String) -> String{
        
        let po = PromiseOut<String>()
        webView.evaluateJavaScript(script) { result, error in
            po.resolver(result as? String ?? "")
        }
        return po.waitPromise() ?? ""
    }
    
    func evaluateAsyncJavascriptCode(script: String, afterEval: @escaping () -> Void) async -> String {
        
        let channel = PassthroughSubject<String,MyError>()
        let id = idAcc
        idAcc += 1
        channelMap[id] = channel
        
        let jsString = """
            void (async()=>{return ($script)})()
                            .then(res=>$JS_ASYNC_KIT.resolve($id,JSON.stringify(res)))
                            .catch(err=>$JS_ASYNC_KIT.reject($id,String(err)));
            """.trimmingCharacters(in: .whitespacesAndNewlines)
        
        await self.webView.callAsyncJavaScript(jsString, arguments: [:], in: nil, in: .world(name: "\(id)")) { result in
            afterEval()
        }
        
        return await withCheckedContinuation { continuation in
            
            cancellable = channel.sink { _ in
                
            } receiveValue: { value in
                continuation.resume(returning: value)
            }
        }
    }
}

extension WebViewEvaluator:  WKScriptMessageHandler {
    
    func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
        
        if message.name == "resolve" {
            print(message.body)
            //点击网页按钮 开始加载
            guard let bodyDict = message.body as? [String:Any] else { return }
            let id = bodyDict["id"] as? Int ?? -1
            let data = bodyDict["data"] as? String ?? ""
            Task {
                let through = channelMap.removeValue(forKey:id)
                if through != nil {
                    through!.send(data)
                    through?.send(completion: .finished)
                }
            }
        } else if message.name == "reject" {
            guard let bodyDict = message.body as? [String:Any] else { return }
            let id = bodyDict["id"] as? Int ?? -1
            let data = bodyDict["reason"] as? String ?? ""
            Task {
                let through = channelMap.removeValue(forKey:id)
                if through != nil {
                    through?.send(completion: .failure(MyError(rawValue: data)!))
                }
            }
        }
    }
}
