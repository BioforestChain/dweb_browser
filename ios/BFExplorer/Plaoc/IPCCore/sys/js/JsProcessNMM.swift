//
//  JsProcessNMM.swift
//  BFExplorer
//
//  Created by ui03 on 2023/3/2.
//

import Foundation
import Vapor
import HandyJSON

class JsProcessNMM: NativeMicroModule {
    
    private let CORS_HEADERS = [
        "Content-Type": "application/javascript",
        "Access-Control-Allow-Origin": "*",
        "Access-Control-Allow-Headers": "*",
        "Access-Control-Allow-Methods": "*"
    ]
    
    private let INTERNAL_PATH = "/<internal>".urlEncoder()
    
    private let dwebServer = HTTPServer()
    
    init() {
        super.init(mmid: "js.sys.dweb")
    }
    
    lazy var JS_PROCESS_WORKER_CODE: String = {
        
        let response = self.nativeFetch(url: URL(string: "file:///bundle/js-process.worker.js")!)
        return response?.body.string ?? ""
    }()
    
    override func _bootstrap(bootstrapContext: BootstrapContext) throws {
        guard let mainServer = self.createHttpDwebServer(options: DwebHttpServerOptions()) else { return }
        
        _ = afterShutdownSignal.listen({ _ in
            mainServer.close
        })
        
        let serverIpc = mainServer.listen()
        _ = serverIpc?.onRequest(cb: { request, ipc in
            guard request.uri != nil else { return }
            if request.uri!.path.starts(with: self.INTERNAL_PATH) {
                var pathString = request.uri?.path ?? ""
                let endIndex = pathString.index(pathString.startIndex, offsetBy: self.INTERNAL_PATH.count)
                pathString = String(pathString[pathString.startIndex..<endIndex])
                request.uri?.path = pathString
                
                let header = IpcHeaders()
                for (key,value) in self.CORS_HEADERS {
                    header.set(key: key, value: value)
                }
                
                if pathString == "/bootstrap.js" {
                    
                    ipc.postMessage(message: IpcResponse.fromText(req_id: request.req_id, text: self.JS_PROCESS_WORKER_CODE, headers: header, ipc: ipc))
                } else {
                    ipc.postMessage(message: IpcResponse.fromText(req_id: request.req_id, text: "// no found \(pathString)", headers: header, ipc: ipc))
                }
                
            } else {
                guard let response = self.nativeFetch(url: URL(string: "file:///bundle/js-process\(request.uri!.path)")!) else { return }
                guard let resp = IpcResponse.fromResponse(req_id: request.req_id, response: response, ipc: ipc) else { return }
                ipc.postMessage(message: resp)
            }
        })
        
        
        var uri = mainServer.startResult.urlInfo.buildInternalUrl()?.toUri()
        uri?.path = "\(INTERNAL_PATH)/bootstrap.js"
        let bootstrap_url = uri?.string ?? ""
        
        let afterReadyPo = PromiseOut<Void>()
        
        let apis = getJsProcessWebApi(urlInfo: mainServer.startResult.urlInfo)
        
        //        _ = afterShutdownSignal.listen { _ in
        //            apis.destroy()
        //            apis.dWebView.onReady { _ in
        //                afterReadyPo.resolver(())
        //            }
        //        }
        //        afterReadyPo.waitPromise()
        
        routerHandler(apis: apis, bootstrap_url: bootstrap_url)
    }
    
    
    private func getJsProcessWebApi(urlInfo: ServerUrlInfo) -> JsProcessWebApi {
        return DispatchQueue.main.sync {
            let api = JsProcessWebApi(dWebView: DWebView(frame: .zero, localeMM: self, remoteMM: self, options: Options(urlString: urlInfo.buildInternalUrl()!.replacePath(replacePath: "/index.html")!.absoluteString)))
            return api
        }
    }
    
    private func routerHandler(apis: JsProcessWebApi, bootstrap_url: String) {
        
        var ipcProcessIdMap: [Ipc: [String:PromiseOut<Int>]] = [:]
        let ipcProcessIdMapLock = NSLock()
        // 创建 web worker
        // request 需要携带一个流，来为 web worker 提供代码服务
        let createProcessRouteHandler: RouterHandler = { (request, ipc) -> InputStream in
            guard ipc != nil else { return }
            let po = ipcProcessIdMapLock.withLock {
                let process_id = request.query[String.self, at: "process_id"]!
                var processIdMap = ipcProcessIdMap[ipc!]
                if processIdMap == nil {
                    _ = ipc?.onClose(cb: { _ in
                        ipcProcessIdMap.removeValue(forKey: ipc!)
                    })
                    ipcProcessIdMap[ipc!] = [:]
                }
                if processIdMap!.keys.contains(process_id) {
                    fatalError("ipc: \(ipc!.remote.mmid)/processId: \(process_id) has already using")
                }
                let pro = PromiseOut<Int>()
                ipcProcessIdMap[ipc!]?[process_id] = pro
                return pro
            }
            
            let entry = request.query[String.self, at: "entry"]
            let result = createProcessAndRun(ipc: ipc!, apis: apis, bootstrap_url: bootstrap_url, entry: entry, requestMessage: request)
            // 将自定义的 processId 与真实的 js-process_id 进行关联
            po.resolver(result?.processHandler)
            // 返回流，因为构建了一个双工通讯用于代码提供服务
            return result?.streamIpc.stream
        }
        // 创建 web 通讯管道
        let createIpcRouteHandler: RouterHandler = { [self] request, ipc in
            let processId = request.query[String.self, at: "process_id"] ?? ""
            /**
             * 虽然 mmid 是从远程直接传来的，但风险与jsProcess无关，
             * 因为首先我们是基于 ipc 来得到 processId 的，所以这个 mmid 属于 ipc 自己的定义
             */
            let mmid = request.query[String.self, at: "mmid"] ?? ""
            let process_id = ipcProcessIdMapLock.withLock {
                ipcProcessIdMap[ipc!]?[processId]
            }?.waitPromise()
            
            if process_id == nil {
                fatalError("ipc: \(ipc!.remote?.mmid)/processId: \(processId ) invalid")
            }
            return self.createIpc(ipc: ipc!, apis: apis, process_id: process_id!, mmid: mmid)
        }
        
        apiRouting["\(self.mmid)/create-process"] = createProcessRouteHandler
        apiRouting["\(self.mmid)/create-ipc"] = createIpcRouteHandler
        
        // 添加路由处理方法到http路由中
        let app = HTTPServer.app
        let group = app.grouped("\(mmid)")
        let httpHandler: (Request) async throws -> Response = { request async in
            await self.defineHandler(request: request)
        }
        group.on(.POST, ["create-process"], use: httpHandler)
        group.on(.GET, ["create-ipc"], use: httpHandler)
    }
    
    override func _shutdown() { }
    
    private func createProcessAndRun(ipc: Ipc, apis: JsProcessWebApi,bootstrap_url: String, entry: String?, requestMessage: Request
    ) async -> CreateProcessAndRunResult? {
        
        //用自己的域名的权限为它创建一个子域名
        let httpDwebServer = createHttpDwebServer(options: DwebHttpServerOptions(subdomain: ipc.remote?.mmid ?? ""))
        
        guard let remote = ipc.remote else { return nil }
        //远端是代码服务，所以这里是 client 的身份
        let streamIpc = ReadableStreamIpc(remote: remote, role: "code-proxy-server")
        
        var data = Data()
        let sequential = requestMessage.eventLoop.makeSucceededFuture(())
        requestMessage.body.drain { result in
            switch result {
            case .buffer(let buffer):
                data = Data(buffer: buffer, byteTransferStrategy: ByteBuffer.ByteTransferStrategy.automatic)
                return sequential
            default:
                return sequential
            }
            let stream = InputStream(data: data)
            streamIpc.bindIncomeStream(stream: stream, coroutineName: streamIpc.role ?? "")
        }
        
        /**
         * 代理监听
         * 让远端提供 esm 模块代码
         * 这里我们将请求转发给对方，要求对方以一定的格式提供代码回来，
         * 我们会对回来的代码进行处理，然后再执行
         */
        let codeProxyServerIpc = httpDwebServer?.listen()
        _ = codeProxyServerIpc?.onRequest(cb: { request, ipc in
            
            if let response = streamIpc.request(request: request.toRequest()) {
                ipc.postResponse(req_id: request.req_id, response: response)
                for (key,value) in self.CORS_HEADERS {
                    response.headers.add(name: key, value: value)
                }
            }
        })
        
        struct JsProcessMetadata: HandyJSON {
            
            init() {
                
            }
            
            var mmid: String
            
            init(mmid: String) {
                self.mmid = mmid
            }
        }
        // TODO 需要传过来，而不是自己构建
        let metadata = JsProcessMetadata(mmid: remote.mmid)
        // TODO env 允许远端传过来扩展
        
        let env = [
            "host": httpDwebServer?.startResult.urlInfo.host,
            "debug": "true",
            "ipc-support-protocols": ""
        ]
        
        /**
         * 创建一个通往 worker 的消息通道
         */
        
        let processHandler = await apis.createProcess(env_script_url: bootstrap_url, metadata_json: metadata.toJSONString() ?? "", env_json: ChangeTools.dicValueString(env) ?? "", remoteModule: remote, host: httpDwebServer?.startResult.urlInfo.host ?? "")
        
        /**
         * 收到 Worker 的数据请求，由 js-process 代理转发回去，然后将返回的内容再代理响应会去
         *
         * TODO 所有的 ipcMessage 应该都有 headers，这样我们在 workerIpcMessage.headers 中附带上当前的 processId，回来的 remoteIpcMessage.headers 同样如此，否则目前的模式只能代理一个 js-process 的消息。另外开 streamIpc 导致的翻译成本是完全没必要的
         */
        
        _ = processHandler.ipc.onMessage { workerIpcMessage, _ in
            /**
             * 直接转发给远端 ipc，如果是nativeIpc，那么几乎没有性能损耗
             */
            ipc.postMessage(message: workerIpcMessage)
        }
        
        _ = ipc.onMessage { remoteIpcMessage, _ in
            processHandler.ipc.postMessage(message: remoteIpcMessage)
        }
        
        /**
         * 开始执行代码
         */
        
        apis.runProcessMain(process_id: processHandler.info.process_id, options: RunProcessMainOptions(main_url: httpDwebServer?.startResult.urlInfo.buildInternalUrl()?.replacePath(replacePath: "/index.js")?.absoluteString ?? ""))
        
        /**
         * “模块之间的IPC通道”关闭的时候，关闭“代码IPC流通道”
         *
         * > 自己shutdown的时候，这些ipc会被关闭
         */
        _ = ipc.onClose { _ in
            streamIpc.closeAction()
        }
        
        _ = streamIpc.onClose { _ in
            httpDwebServer?.close
        }
        
        return CreateProcessAndRunResult(streamIpc: streamIpc, processHandler: processHandler)
    }
    
    func createIpc(ipc: Ipc, apis: JsProcessWebApi, process_id: Int, mmid: String) -> Int {
        return apis.createIpc(process_id: process_id, mmid: mmid)
    }
}


struct CreateProcessAndRunResult {
    
    var streamIpc: ReadableStreamIpc
    var processHandler: ProcessHandler
    
    init(streamIpc: ReadableStreamIpc, processHandler: ProcessHandler) {
        self.streamIpc = streamIpc
        self.processHandler = processHandler
    }
}
