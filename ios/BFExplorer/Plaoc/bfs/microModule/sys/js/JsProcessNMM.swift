//
//  JsProcessNMM.swift
//  BFExplorer
//
//  Created by ui08 on 2023/2/2.
//

import WebKit
import Foundation
import Vapor
import Flow

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
    
    private var JS_PROCESS_WORKER_CODE: String {
        get async {
            await nativeFetch(url: "file:///bundle/js-process.worker.js").text()!
        }
    }
    
    private let CORS_HEADERS = [
        "Content-Type": "application/javascript",
        "Access-Control-Allow-Origin": "*",
        "Access-Control-Allow-Headers": "*",
        "Access-Control-Allow-Methods": "*"
    ]
    
    private let INTERNAL_PATH = "/<internal>".encodeURI()
    
    private func getJsProcessWebApi(urlInfo: HttpNMM.ServerUrlInfo) -> JsProcessWebApi {
        return DispatchQueue.main.sync {
            let api = JsProcessWebApi(webView: DWebView(mm: self, options: DWebView.Options(url: urlInfo.buildInternalUrl().replacePath("/index.html").absoluteString), frame: .zero))
//                _afterShutdownSignal.listen {}
            return api
        }
    }
    
    override func _bootstrap(bootstrapContext: BootStrapContext) async throws {
        /// 主页的网页服务
        let mainServer = await self.createHttpDwebServer(options: DwebHttpServerOptions())
        _ = _afterShutdownSignal.listen {
            mainServer.close()
            return nil
        }
        
        // 提供基本的主页服务
        let serverIpc = await mainServer.listen()
        _ = serverIpc.onRequest { ipcReqMessage, ipc in
            let ipcRequest = ipcReqMessage.toIpcRequest(ipc: ipc)
            
            // <internal>开头的是特殊路径，给Worker用的，不会拿去请求文件
            if ipcRequest.uri!.path.hasPrefix(self.INTERNAL_PATH) {
                let internalUri = URL(string: ipcRequest.uri!.path)!
                    .replacePath(ipcRequest.uri!.path.slice(self.INTERNAL_PATH.count, -1))
                
                if internalUri.path == "/bootstrap.js" {
                    return await ipc.postMessage(message: IpcResponse.fromText(
                        req_id: ipcRequest.req_id,
                        text: await self.JS_PROCESS_WORKER_CODE,
                        headers: .init(self.CORS_HEADERS),
                        ipc: ipc).ipcResMessage)
                } else {
                    return await ipc.postMessage(message: IpcResponse.fromText(
                        req_id: ipcRequest.req_id,
                        text: "// no found \(internalUri.path)",
                        headers: .init(self.CORS_HEADERS),
                        ipc: ipc).ipcResMessage)
                }
            } else {
                let response = await self.nativeFetch(url: "file:///bundle/js-process\(ipcRequest.uri!.path)")
                return await ipc.postMessage(message: IpcResponse.fromResponse(
                    req_id: ipcRequest.req_id,
                    response: response,
                    ipc: ipc).ipcResMessage)
            }
        }
        
        let bootstrap_url = mainServer.startResult.urlInfo.buildInternalUrl()
            .replacePath("\(INTERNAL_PATH)/bootstrap.js").absoluteString
        
        print("mainServer: \(mainServer.startResult)")
        
        let apis = getJsProcessWebApi(urlInfo: mainServer.startResult.urlInfo)
        
        var ipcProcessIdMap: [Ipc:[String:PromiseOut<Int>]] = [:]
        let ipcProcessIdMapLock = NSLock()
        /// 创建 web worker
        // request 需要携带一个流，来为 web worker 提供代码服务
        let createProcessRouteHandler: RouterHandler = { request, ipc async in
            let po = ipcProcessIdMapLock.withLock {
                let process_id = request.query[String.self, at: "process_id"]!
                var processIdMap = ipcProcessIdMap[ipc!]
                if processIdMap == nil {
                    _ = ipc!.onClose {
                        ipcProcessIdMap.removeValue(forKey: ipc!)
                        return .OFF
                    }
                    ipcProcessIdMap[ipc!] = [:]
                    processIdMap = [:]
                }
                if processIdMap!.keys.contains(where: { $0 == process_id }) {
                    fatalError("ipc: \(ipc!.remote.mmid)/processId: \(process_id) has already using")
                }
                
                let po = PromiseOut<Int>()
                ipcProcessIdMap[ipc!]![process_id] = po
                
                return po
            }
            
            let entry = request.query[String.self, at: "entry"]
            
            let result = await self.createProcessAndRun(
                ipc: ipc!,
                apis: apis,
                bootstrap_url: bootstrap_url,
                entry: entry,
                requestMessage: request)
            
            // 返回流，因为构建了一个双工通讯用于代码提供服务
            po.resolve(result.processHandler.info.process_id)
            
            // 返回流，因为构建了一个双工通讯用于代码提供服务
            return result.streamIpc.stream
        }
        /// 创建 web 通讯管道
        let createIpcRouteHandler: RouterHandler = { request, ipc async in
            let processId = request.query[String.self, at: "process_id"]
            
            /**
             * 虽然 mmid 是从远程直接传来的，但风险与jsProcess无关，
             * 因为首先我们是基于 ipc 来得到 processId 的，所以这个 mmid 属于 ipc 自己的定义
             */
            let mmid = request.query[Mmid.self, at: "mmid"]
            let process_id = await ipcProcessIdMapLock.withLock {
                ipcProcessIdMap[ipc!]?[processId!]
            }?.waitPromise()
            
            if process_id == nil {
                fatalError("ipc: \(ipc!.remote.mmid)/processId: \(processId ?? "") invalid")
            }
            
            return await self.createIpc(ipc: ipc!, apis: apis, process_id: process_id!, mmid: mmid!)
        }
        apiRouting["\(self.mmid)/create-process"] = createProcessRouteHandler
        apiRouting["\(self.mmid)/create-ipc"] = createIpcRouteHandler
        
        // 添加路由处理方法到http路由中
        let app = HttpServer.app
        let group = app.grouped("\(mmid)")
        let httpHandler: (Request) async throws -> Response = { request async in
            await self.defineHandler(request: request)
        }
        group.on(.POST, ["create-process"], use: httpHandler)
        group.on(.GET, ["create-ipc"], use: httpHandler)
    }
    
    private func createProcessAndRun(
        ipc: Ipc,
        apis: JsProcessWebApi,
        bootstrap_url: String,
        entry: String?,
        requestMessage: Request
    ) async -> CreateProcessAndRunResult {
        /**
         * 用自己的域名的权限为它创建一个子域名
         */
        let httpDwebServer = await createHttpDwebServer(options: DwebHttpServerOptions(subdomain: ipc.remote.mmid))
        
        /**
         * 远端是代码服务，所以这里是 client 的身份
         */
        let streamIpc = ReadableStreamIpc(remote: ipc.remote, role: "code-proxy-server")
//        var data = Data()
//        let sequential = requestMessage.eventLoop.makeSucceededFuture(())
//        requestMessage.body.drain {
//            switch $0 {
//            case .buffer(var buffer):
//                let _data = buffer.readData(length: buffer.readableBytes)
//                if _data != nil {
//                    data.append(_data!)
//                }
//                return sequential
//            case .error, .end:
//                return sequential
//            }
//        }
//
//        let stream = InputStream(data: data)
//        await streamIpc.bindIncomeStream(stream: stream)
        var buffer = try? await requestMessage.body.collect().get()
        await streamIpc.bindIncomeStream(data: buffer!.readableBytes > 0 ? buffer!.readData(length: buffer!.readableBytes)! : nil)
        
        /**
         * 代理监听
         * 让远端提供 esm 模块代码
         * 这里我们将请求转发给对方，要求对方以一定的格式提供代码回来，
         * 我们会对回来的代码进行处理，然后再执行
         */
        let codeProxyServerIpc = await httpDwebServer.listen()
        _ = codeProxyServerIpc.onRequest { ipcReqMessage, ipc in
            // 转发给远端来处理
            let response = await streamIpc.request(request: ipcReqMessage.toIpcRequest(ipc: ipc).toRequest())
            for (key, value) in self.CORS_HEADERS {
                response.headers.add(name: key, value: value)
            }
            await ipc.postResponse(req_id: ipcReqMessage.req_id, response: response)
            return nil
        }
        
        struct JsProcessMetadata: Codable {
            let mmid: Mmid
        }
        
        // TODO: 需要传过来，而不是自己构建
        let metadata = JsProcessMetadata(mmid: ipc.remote.mmid)
        
        let env = [
            "host": httpDwebServer.startResult.urlInfo.host,
            "debug": "true",
            "ipc-support-protocols": ""
        ]
        
        /**
         * 创建一个通往 worker 的消息通道
         */
        let processHandler = await apis.createProcess(
            env_script_url: bootstrap_url,
            metadata_json: JSONStringify(metadata)!,
            env_json: ChangeTools.dicValueString(env)!,
            remoteModule: ipc.remote,
            host: httpDwebServer.startResult.urlInfo.host
        )
        
        /**
         * 收到 Worker 的数据请求，由 js-process 代理转发回去，然后将返回的内容再代理响应会去
         *
         * TODO:  所有的 ipcMessage 应该都有 headers，这样我们在 workerIpcMessage.headers 中附带上当前的 processId，回来的 remoteIpcMessage.headers 同样如此，否则目前的模式只能代理一个 js-process 的消息。另外开 streamIpc 导致的翻译成本是完全没必要的
         */
        _ = processHandler.ipc.onMessage { (workerIpcMessage, _) in
            /**
             * 直接转发给远端 ipc，如果是nativeIpc，那么几乎没有性能损耗
             */
            await ipc.postMessage(message: workerIpcMessage)
        }
        _ = ipc.onMessage { (remoteIpcMessage, _) in
            await processHandler.ipc.postMessage(message: remoteIpcMessage)
        }
        
        /**
         * 开始执行代码
         */
        await apis.runProcessMain(
            process_id: processHandler.info.process_id,
            options: JsProcessWebApi.RunProcessMainOptions(
                main_url: httpDwebServer.startResult.urlInfo.buildInternalUrl().replacePath(entry ?? "/index.js").absoluteString))
        
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

        return CreateProcessAndRunResult(streamIpc: streamIpc, processHandler: processHandler)
    }
    
    struct CreateProcessAndRunResult {
        let streamIpc: ReadableStreamIpc
        let processHandler: JsProcessWebApi.ProcessHandler
    }
    
    private func createIpc(
        ipc: Ipc,
        apis: JsProcessWebApi,
        process_id: Int,
        mmid: Mmid
    ) async -> Int {
        return await apis.createIpc(process_id: process_id, mmid: mmid)
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
            let streamIpc = await nmm.listenHttpDwebServer(startResult: startResult, routes: routes)
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
        await nativeFetch(url: URL(string: "file://http.sys.dweb/start")!
            .appending("port", value: "\(options.port)")
            .appending("subdomain", value: options.subdomain.encodeURIComponent())
            .absoluteString)
        .json(HttpNMM.ServerStartResult.self)
    }
    
    func listenHttpDwebServer(startResult: HttpNMM.ServerStartResult, routes: [Gateway.RouteConfig]) async -> ReadableStreamIpc {
        let ipc = ReadableStreamIpc(remote: self, role: "http-server/\(startResult.urlInfo.host)")
        let response = await nativeFetch(request: Request.new(
            method: .POST,
            url: URL(string: "file://http.sys.dweb/listen")!
                .appending("host", value: startResult.urlInfo.host.encodeURIComponent())
                .appending("token", value: startResult.token.encodeURIComponent())
                .appending("routes", value: JSONStringify(routes)!).absoluteString)
        )
        var buffer = try? await response.body.collect(on: HttpServer.app.eventLoopGroup.next()).get()
        await ipc.bindIncomeStream(data: buffer!.readableBytes > 0 ? buffer!.readData(length: buffer!.readableBytes)! : nil)
        return ipc
    }
    
    func closeHttpDwebServer(options: DwebHttpServerOptions) async -> Bool {
        await nativeFetch(url: URL(string: "file://http.sys.dweb/close")!
            .appending("port", value: "\(options.port)")
            .appending("subdomain", value: options.subdomain.encodeURIComponent())
            .absoluteString)
        .boolean()
    }
}

