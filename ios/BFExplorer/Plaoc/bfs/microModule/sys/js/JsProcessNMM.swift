//
//  JsProcessNMM.swift
//  BFExplorer
//
//  Created by ui08 on 2023/2/2.
//

import WebKit
import Foundation
import Vapor

class JsProcessNMM: NativeMicroModule {
    private var nww: WKWebView? = nil
    private lazy var webView: WKWebView = {
        return WKWebView(frame: .zero)
    }()
    
    override init() {
        super.init()
        mmid = "js.sys.dweb"
    }
    
    override func _bootstrap() async throws {
        // 必须要为每个js空间注册，否则无法使用
        nww = webView
        
    }
    
    
}

class HttpDwebServer {
    private let nmm: MicroModule
    private let options: DwebHttpServerOptions
    let startResult: HttpNMM.ServerStartResult
    
    init(nmm: MicroModule, options: DwebHttpServerOptions, startResult: HttpNMM.ServerStartResult) {
        self.nmm = nmm
        self.options = options
        self.startResult = startResult
    }
    
    func listen() async -> ReadableStreamIpc {
        return await withCheckedContinuation { continuation in
            
        }
    }
}


extension MicroModule {
    func createHttpDwebServer(options: DwebHttpServerOptions) {
        //        Http
    }
    
    func startHttpDwebServer(options: DwebHttpServerOptions) {
        self.nativeFetch(url: URI(string: "file://http.sys.dweb/start?port=\(options.port)&subdomain=\(options.subdomain)"))
    }
    
    func listenHttpDwebServer(token: String) {
        let ipc = ReadableStreamIpc(remote: self, role: .client)
        ipc.bindIncomeStream(stream: self.nativeFetch(request: Request(application: HttpServer.app, url: URI(string: "file://http.sys.dweb/listen?token=\(token)&routes="), on: HttpServer.app.eventLoopGroup.next())))
    }
    
    func closeHttpDwebServer(options: DwebHttpServerOptions) {
        
    }
}


//class JsProcessNMM: NativeMicroModule {
//    private lazy var webview: WKWebView = {
//        return WKWebView(frame: .zero)
//    }()
//
//    var all_ipc_cache: [Int:NativeIpc] = [:]
//
//    private var acc_process_id = 0
//
//    convenience init() {
//        self.init(mmid: "js.sys.dweb")
//        _ = webview
//
//        Routers["/create-process"] = { args in
//            guard let args = args as? [String:String] else { return nil }
//
//            if args["main_pathname"] == nil {
//                return nil
//            }
//
//            let process_id = self.acc_process_id++
//
//            // 必须要为每个js空间注册，否则无法使用
//            self.webview.configuration.userContentController.add(LeadScriptHandle(messageHandle: self), contentWorld: WKContentWorld.world(name: String(process_id)), name: "webworkerOnmessage")
//            self.webview.configuration.userContentController.add(LeadScriptHandle(messageHandle: self), contentWorld: WKContentWorld.world(name: String(process_id)), name: "logging")
//            self.webview.configuration.userContentController.add(LeadScriptHandle(messageHandle: self), contentWorld: WKContentWorld.world(name: String(process_id)), name: "portForward")
//
//            self.hookJavascriptWorker(process_id: process_id, main_pathname: args["main_pathname"]!)
//
//            return process_id
//        }
//        Routers["/create-ipc"] = { args in
//            guard let args = args as? [String:Int], let process_id = args["worker_id"] else { return nil }
//
//            if self.all_ipc_cache.index(forKey: process_id) == nil {
//                print("JsProcessNMM create-ipc no found worker by id '\(process_id)'")
//                return nil
//            }
//
//            let text = #"""
//                port1.onmessage = (evt) => {
//                    evt.data["process_id"] = \(process_id);
//                    window.webkit.messageHandlers.portForward.postMessage(evt.data);
//                }
//            """#
//            self.evaluateJavaScript(text: text, process_id: process_id)
//
//            return process_id
//        }
//    }
//
//    override func _bootstrap() -> Any {
//        return true
//    }
//
//    func evaluateJavaScript(text: String, process_id: Int) {
//        DispatchQueue.main.async {
//            self.webview.evaluateJavaScript(text, in: nil, in: WKContentWorld.world(name: String(process_id))) { result in
//                switch result {
//                case .success(let suc):
//                    print("suc: \(suc)")
//                case .failure(let err):
//                    print(err.localizedDescription)
//                }
//            }
//        }
//    }
//
//    func hookJavascriptWorker(process_id: Int, main_pathname: String) {
//        DispatchQueue.global().async {
//            do {
//                let main_code = try String(contentsOf: main_pathname.hasPrefix("http") ? URL(string: main_pathname)! : URL(fileURLWithPath: main_pathname), encoding: .utf8)
//                let injectWorkerDir = URL(fileURLWithPath: Bundle.main.bundlePath + "/app/injectWebView/worker.js")
//                let injectWorkerCode = try String(contentsOf: injectWorkerDir, encoding: .utf8).replacingOccurrences(of: "\"use strict\";", with: "")
//                let workerCode = """
//                    data:utf-8,
//                 ((module,exports=module.exports)=>{\(injectWorkerCode.encodeURIComponent());return module.exports})({exports:{}}).installEnv();
//                 \(main_code.encodeURIComponent())
//                """
//
//                let text = """
//                    window.webkit.messageHandlers.logging.postMessage('xxxxxxxx');
//                    const webworker = new Worker(`\(workerCode)`);
//                    try {
//                        webworker.onmessage = (evt) => {
//                            if(typeof evt.data === 'string') {
//                                window.webkit.messageHandlers.logging.postMessage(evt.data);
//                            } else {
//                                evt.data['process_id'] = \(process_id);
//                                window.webkit.messageHandlers.webworkerOnmessage.postMessage(JSON.stringify(evt.data));
//                            }
//
//                        }
//                    } catch(e) {
//                        window.webkit.messageHandlers.logging.postMessage('error');
//                        window.webkit.messageHandlers.logging.postMessage(e.message);
//                    }
//                    ''
//                """
//                ClipboardManager.write(content: text, ofType: .string)
//                self.evaluateJavaScript(text: text, process_id: process_id)
//            } catch {
//                print("JsProcessNMM hookJavascriptWorker error: \(error)")
//            }
//        }
//    }
//
//    // ipc请求数据响应内容返回
//    func ipcResponseMessage(res: IpcResponse, process_id: Int) {
//        print(res.toDic())
//        let resStr = ChangeTools.dicValueString(res.toDic())
//        let text = """
//            webworker.postMessage(['ipc-response', \(resStr!)], [\(resStr!)]);
//            ''
//        """
//
//        self.evaluateJavaScript(text: text, process_id: process_id)
//    }
//}
//
//extension JsProcessNMM: WKScriptMessageHandler {
//    func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
//        if message.name == "webworkerOnmessage" {
//            print("webworkerOnmessage")
//            guard let reqBody = message.body as? String else { return }
//            let args = JSON.init(parseJSON: reqBody)
//            let url = args["url"].stringValue
//            let process_id = args["process_id"].intValue
//
////            if self.all_ipc_cache.index(forKey: process_id) == nil { return }
//
//            print(url)
//            let resBody = DnsNMM.shared.nativeFetch(urlString: url, microModule: self)
//
//            var res: IpcResponse
//            let req_id = args["req_id"].intValue
//            let headers = ["Content-Type":"text/plain"]
////            do {
//                if let body = resBody as? String {
//                    res = IpcResponse(req_id: req_id, statusCode: 200, body: body, headers: headers)
//                } else if let body = resBody as? [String:Any] {
//                    res = IpcResponse(req_id: req_id, statusCode: 200, body: ChangeTools.dicValueString(body) ?? "", headers: headers)
//                } else if resBody != nil {
////                    try res = IpcResponse(req_id: req_id, statusCode: 200, body: "\(resBody)", headers: headers)
//                    res = IpcResponse(req_id: req_id, statusCode: 200, body: "\(resBody!)", headers: headers)
//                } else {
//                    res = IpcResponse(req_id: req_id, statusCode: 404, body: "no found handler for \(args["pathname"].stringValue)", headers: headers)
//                }
////            } catch let err {
////                res = IpcResponse(req_id: req_id, statusCode: 500, body: "\(err)", headers: headers)
////            }
//
//            self.ipcResponseMessage(res: res, process_id: process_id)
//        } else if(message.name == "logging") {
//            print(message.body)
//        } else if(message.name == "portForward") {
//            print("portForward")
//            guard let data = message.body as? [String:Any], let process_id = data["process_id"] as? Int else { return }
//
//            let port1 = "\(process_id)_port1"
//            let port2 = "\(process_id)_port2"
//            let ipc = JsIpc(port1: port1, port2: port2)
//            self.all_ipc_cache[process_id] = ipc
//        }
//    }
//}
                    
                    
