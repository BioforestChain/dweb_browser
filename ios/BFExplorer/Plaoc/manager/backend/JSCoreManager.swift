//
//  JSCoreHandle.swift
//  Plaoc-iOS
//
//  Created by ui03 on 2022/11/4.
//

import Foundation
import JavaScriptCore

class JSCoreManager: NSObject {
    
    private var baseViewController: WebViewViewController?
    private let jsContext = JSContext()
    private var plaoc = PlaocHandleModel()
    private var name: String = ""
    private var postDict: [String:String] = [:]
    
    
    init(appId: String, controller: WebViewViewController?) {
        super.init()
        baseViewController = controller
        name = appId
        
        plaoc.controller = baseViewController
        plaoc.jsContext = jsContext
        plaoc.appId = name
        
        JSInjectManager.shared.registerInContext(jsContext!)
        initJSCore()
//        loadAPPEntry(appId: appId)

    }
    /**初始化 sdk*/
    private func initJSCore() {
        // inject TextDecode
        injectJsContext("/injectJsCore/encoding-indexes.js");
        injectJsContext("/injectJsCore/encoding.js");
        
        injectJsContext("/injectJsCore/URL.js");
        
        injectJsContext("/sdk/HE74YAAL/boot/index.js");
    }
    /**注入javascriptCore*/
    private func injectJsContext(_ js:String) {
        let entryPath = Bundle.main.bundlePath + "/app" + js
        
        jsContext?.setObject(plaoc, forKeyedSubscript: "PlaocJavascriptBridge" as NSCopying & NSObjectProtocol)
        if let content = try? String(contentsOfFile: entryPath) {
            jsContext?.evaluateScript(content)
        }
    }
    
    private func loadAPPEntry(appId: String) {
        
        guard let entryPath = InnerAppFileManager.shared.systemAPPEntryPath(appId: appId) else { return }
        
        jsContext?.setObject(plaoc, forKeyedSubscript: "PlaocJavascriptBridge" as NSCopying & NSObjectProtocol)
        if let content = try? String(contentsOfFile: entryPath) {
            jsContext?.evaluateScript(content)
        }
    }
    
    func callFunction<T>(functionName: String, withData dataObject: Codable, type: T.Type) -> JSValue? where T:Codable {
        var dataString = ""
        if let string = getString(fromObject: dataObject, type: type) {
            dataString = string
        }
        let functionString = functionName + "(\(dataString)"
        let result = jsContext?.evaluateScript(functionString)
        return result
    }
    
    // 处理get请求
    func handleEvaluateScript(jsString: String) {
        let functionString = "webView.getIosMessage('\(jsString)')"
        let result = jsContext?.evaluateScript(functionString)
        print("swift#handleEvaluateScript",functionString)
//        print(result?.toString())
    }
    
    // 处理post请求
    func handleEvaluatePostScript(strPath: String, cmd: String, buffer: String) {
        
        var postString = postDict[cmd] ?? ""
        
        if buffer != "0" {
            if postString.count > 0 {
                postString += ",\(buffer)"
            } else {
                postString += buffer
            }
            postDict[cmd] = postString
        } else {
            let functionString = "webView.getIosMessage('\(strPath)', '\(postString)')"
            let result = jsContext?.evaluateScript(functionString)
            print("swift#handleEvaluatePostScript", functionString)
            postDict.removeValue(forKey: cmd)
        }
    }
    
    // 处理监听事件
    func handleEvaluateEmitScript(wb: String, fun: String, data: Any) {
        let functionString = "javascript:document.querySelector('\(wb)').notifyListeners('\(fun)','\(data)')"
        jsContext?.evaluateScript(functionString)
    }
    
    // 异步返回结果
    func asyncReturnValue(functionName: String, result: Any) {
        self.jsContext?.evaluateScript("callDwebViewFactory('\(functionName)', '\(result)')")
    }
}

extension JSCoreManager {
    
    private func getString<T>(fromObject jsonObject: Codable, type: T.Type) -> String? where T:Codable {
        let encoder = JSONEncoder()
        guard let dataObj = jsonObject as? T,
              let data = try? encoder.encode(dataObj),
              let string = String(data: data, encoding: .utf8) else { return nil }
        return string
    }
    
    
}
