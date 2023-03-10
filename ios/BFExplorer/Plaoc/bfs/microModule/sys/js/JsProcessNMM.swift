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
//    private var nww: WKWebView? = nil
//    private lazy var webView: WKWebView = {
//        return WKWebView(frame: .zero)
//    }()
    
    override init() {
        super.init()
        mmid = "js.sys.dweb"
    }
    
//    private lazy var JS_PROCESS_WORKER_CODE: String = {
//        nativeFetch(url: "file:///bundle/js-process.worker.js").text()!
//    }()
    
    private let CORS_HEADERS = [
        "Content-Type": "application/javascript",
        "Access-Control-Allow-Origin": "*",
        "Access-Control-Allow-Headers": "*",
        "Access-Control-Allow-Methods": "*"
    ]
    
    private let INTERNAL_PATH = "/<internal>".encodeURIComponent()
    
    private func getJsProcessWebApi(urlInfo: HttpNMM.ServerUrlInfo) -> JsProcessWebApi {
        return DispatchQueue.main.sync {
            let api = JsProcessWebApi(webView: DWebView(mm: self, options: DWebView.Options(url: urlInfo.buildInternalUrl().replacePath("/index.html").absoluteString), frame: .zero))
//                _afterShutdownSignal.listen {}
            return api
        }
    }
    
    override func _bootstrap() async throws {
        // 必须要为每个js空间注册，否则无法使用
//        nww = webView
        let mainServer = await self.createHttpDwebServer(options: DwebHttpServerOptions())
        _ = _afterShutdownSignal.listen {
            mainServer.close()
            return nil
        }
        
        let serverIpc = await mainServer.listen()
        _ = serverIpc.onRequest { request, ipc in
            let response = await self.nativeFetch(url: "file:///bundle/js-process\(request.uri!.path)")
            await ipc.postMessage(message: IpcResponse.fromResponse(req_id: request.req_id, response: response, ipc: ipc).ipcResMessage)
            return nil
        }
        
        let apis = getJsProcessWebApi(urlInfo: mainServer.startResult.urlInfo)
        
        let createProcessRouteHandler: RouterHandler = { request, ipc async in
            let main_pathname = request.query[String.self, at: "main_pathname"]
            return await self.createProcessAndRun(ipc: ipc!, apis: apis, main_pathname: main_pathname!, requestMessage: request)
        }
        let createIpcRouteHandler: RouterHandler = { request, _ async in
            let process_id = request.query[Int.self, at: "process_id"]
            await apis.createIpc(process_id: process_id!)
            return Response(status: .ok)
        }
        apiRouting["\(self.mmid)/create-process"] = createProcessRouteHandler
        apiRouting["\(self.mmid)/create-ipc"] = createIpcRouteHandler
        
        // 添加路由处理方法到http路由中
        let app = HttpServer.app
        let group = app.grouped("\(mmid)")
        let httpHandler: (Request) async throws -> Response = { request async in
            await self.defineHandler(request: request)
        }
        for pathComponent in ["create-process", "create-ipc"] {
            group.on(.GET, [PathComponent(stringLiteral: pathComponent)], use: httpHandler)
        }
    }
    
    private func createProcessAndRun(
        ipc: Ipc,
        apis: JsProcessWebApi,
        main_pathname: String = "/index.js",
        requestMessage: Request
    ) async -> Response {
        /**
         * 用自己的域名的权限为它创建一个子域名
         */
        let httpDwebServer = await createHttpDwebServer(options: DwebHttpServerOptions(subdomain: ipc.remote.mmid))
        
        /**
         * 远端是代码服务，所以这里是 client 的身份
         */
        let streamIpc = ReadableStreamIpc(remote: ipc.remote, role: .client)
        var data = Data()
        let sequential = requestMessage.eventLoop.makeSucceededFuture(())
        requestMessage.body.drain {
            switch $0 {
            case .buffer(var buffer):
                let _data = buffer.readData(length: buffer.readableBytes)
                if _data != nil {
                    data.append(_data!)
                }
                return sequential
            case .error, .end:
                return sequential
            }
        }
        
        let stream = InputStream(data: data)
        await streamIpc.bindIncomeStream(stream: stream)
        
        /**
         * 代理监听
         * 让远端提供 esm 模块代码
         * 这里我们将请求转发给对方，要求对方以一定的格式提供代码回来，
         * 我们会对回来的代码进行处理，然后再执行
         */
        let codeProxyServerIpc = await httpDwebServer.listen()
        let JS_PROCESS_WORKER_CODE = await nativeFetch(url: "file:///bundle/js-process.worker.js").text()!
        _ = codeProxyServerIpc.onRequest { request, ipc in
            if request.uri!.path.hasPrefix(self.INTERNAL_PATH) {
                let internalUri = request.uri!.path.slice(0, self.INTERNAL_PATH.count).pathComponents.string
                
                if internalUri == "/bootstrap.js" {
                    await ipc.postMessage(message: IpcResponse.fromText(req_id: request.req_id, statusCode: 200, text: JS_PROCESS_WORKER_CODE, headers: IpcHeaders(self.CORS_HEADERS), ipc: ipc).ipcResMessage)
                } else {
                    await ipc.postMessage(message: IpcResponse.fromText(req_id: request.req_id, statusCode: 404, text: "// no found \(internalUri)", headers: IpcHeaders(self.CORS_HEADERS), ipc: ipc).ipcResMessage)
                }
            } else {
                Task {
                    let response = await streamIpc.request(request: request.toRequest())
                    for (key, value) in self.CORS_HEADERS {
                        response.headers.add(name: key, value: value)
                    }
                    
                    await ipc.postResponse(req_id: request.req_id, response: response)
                }
            }
            
            return nil
        }
        
        let bootstrap_url = httpDwebServer.startResult.urlInfo.buildInternalUrl()
            .replacePath("\(INTERNAL_PATH)/bootstrap.js")
            .appending("mmid", value: ipc.remote.mmid)
            .appending("host", value: httpDwebServer.startResult.urlInfo.host)
            .absoluteString
        
        /**
         * 创建一个通往 worker 的消息通道
         */
        let processHandler = await apis.createProcess(env_script_url: bootstrap_url, remoteModule: ipc.remote)
        
        /// 收到 Worker 的数据请求，由 js-process 代理转发出去，然后将返回的内容再代理响应会去
        // TODO: 跟 dns 要 jmmMetadata 信息然后进行路由限制 eg: jmmMetadata.permissions.contains(ipcRequest.uri.host) // ["camera.sys.dweb"]
        _ = processHandler.ipc.onRequest { ipcRequest, ipc in
            let request = ipcRequest.toRequest()
            let response = await ipc.remote.nativeFetch(request: request)
            let ipcResponse = IpcResponse.fromResponse(req_id: ipcRequest.req_id, response: response, ipc: ipc)
            
            await ipc.postMessage(message: ipcResponse.ipcResMessage)
            
            return nil
        }
        
        /**
         * 开始执行代码
         */
        await apis.runProcessMain(process_id: processHandler.info.process_id, options: JsProcessWebApi.RunProcessMainOptions(main_url: httpDwebServer.startResult.urlInfo.buildInternalUrl().replacePath(main_pathname).absoluteString))
        
        /// 绑定销毁
        /**
         * “模块之间的IPC通道”关闭的时候，关闭“代码IPC流通道”
         *
         * > 自己shutdown的时候，这些ipc会被关闭
         */
        _ = ipc.onClose {
            Task {
                await streamIpc.close()
            }
            return .OFF
        }

        /**
         * “代码IPC流通道”关闭的时候，关闭这个子域名
         */
        _ = streamIpc.onClose {
            httpDwebServer.close()
            return .OFF
        }

        /// 返回自定义的 Response，里头携带我们定义的 ipcStream
        return Response(status: .ok, body: .init(stream: { writer in
            let stream = streamIpc.stream
            let bufferSize = 1024
            stream.open()
            
            while stream.hasBytesAvailable {
                var data = Data()
                var buffer = [UInt8](repeating: 0, count: bufferSize)
                let bytesRead = stream.read(&buffer, maxLength: bufferSize)
                if bytesRead < 0 {
                    stream.close()
                    _ = writer.write(.error("Error reading from stream" as! Error))
                } else if bytesRead == 0 {
                    stream.close()
                    _ = writer.write(.end)
                }
                data.append(buffer, count: bytesRead)
                let byteBuffer = ByteBuffer(data: data)
                _ = writer.write(.buffer(byteBuffer))
            }
        }))
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
    
    func listen(routes: [Gateway.RouteConfig] = [
        Gateway.RouteConfig(pathname: "", method: .GET),
        Gateway.RouteConfig(pathname: "", method: .POST),
        Gateway.RouteConfig(pathname: "", method: .PUT),
        Gateway.RouteConfig(pathname: "", method: .DELETE),
    ]) async -> ReadableStreamIpc {
        let po = PromiseOut<ReadableStreamIpc>()
        
        Task {
            let streamIpc = await nmm.listenHttpDwebServer(token: startResult.token, routes: routes)
            po.resolve(streamIpc)
        }
        
        return await po.waitPromise()
    }
    
    func close() {
        Task {
            await nmm.closeHttpDwebServer(options: options)
        }
    }
}


extension MicroModule {
    func createHttpDwebServer(options: DwebHttpServerOptions) async -> HttpDwebServer {
        HttpDwebServer(nmm: self, options: options, startResult: await startHttpDwebServer(options: options))
    }
    
    func startHttpDwebServer(options: DwebHttpServerOptions) async -> HttpNMM.ServerStartResult {
        await nativeFetch(url: URI(string: "file://http.sys.dweb/start?port=\(options.port)&subdomain=\(options.subdomain)")).json(HttpNMM.ServerStartResult.self)
    }
    
    func listenHttpDwebServer(token: String, routes: [Gateway.RouteConfig]) async -> ReadableStreamIpc {
        let ipc = ReadableStreamIpc(remote: self, role: .client)
        await ipc.bindIncomeStream(stream: await nativeFetch(request: Request.new(url: "file://http.sys.dweb/listen?token=\(token)&routes=\(ChangeTools.arrayValueString(routes)!)")).stream())
        return ipc
    }
    
    func closeHttpDwebServer(options: DwebHttpServerOptions) async -> Bool {
        await nativeFetch(url: URI(string: "file://http.sys.dweb/close?port=\(options.port)&subdomain=\(options.subdomain)")).boolean()
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
                    
                    
