//
//  JsProcessNMM.swift
//  BFExplorer
//
//  Created by ui03 on 2023/3/2.
//

import Foundation
import Vapor

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
    
    override func _bootstrap() throws {
        let mainServer = self.createHttpDwebServer(options: DwebHttpServerOptions())
        if mainServer != nil {
            _ = afterShutdownSignal.listen { _ in
                let callback = {
                    mainServer?.close
                }
                let generics = GenericsClosure(closure: callback)
                return generics
            }
            
            let serverIpc = mainServer?.listen()
            _ = serverIpc?.onRequest(cb: { request, ipc in
                guard let response = self.nativeFetch(url: URL(string: "file:///bundle/js-process\(request.url!.path)")!) else { return }
                guard let resp = IpcResponse.fromResponse(req_id: request.req_id, response: response, ipc: ipc) else { return }
                ipc.postMessage(message: resp)
            })
        }
        
        //TODO WebView 实例
        
        let app = dwebServer.app
        let group = app.grouped("\(mmid)")
        
        group.on(.POST, "create-process") { request -> Response in
            let response = self.defineHandler(request: request) { reque in
                
                return true
            }
            return response
        }
        
        group.on(.GET, "create-ipc") { request -> Response in
            let response = self.defineHandler(request: request) { reque in
                
            }
            return response
        }
    }
    
    override func _shutdown() { }
    
    private func createProcessAndRun(ipc: Ipc, apis: JsProcessWebApi,m ain_pathname: String = "/index.js", requestMessage: URLRequest
    ) -> Response? {
        
        //用自己的域名的权限为它创建一个子域名
        let httpDwebServer = createHttpDwebServer(options: DwebHttpServerOptions(subdomain: ipc.remote?.mmid ?? ""))
        
        guard let remote = ipc.remote else { return nil }
        //远端是代码服务，所以这里是 client 的身份
        let streamIpc = ReadableStreamIpc(remote: remote, role: .CLIENT)
        streamIpc.bindIncomeStream(stream: requestMessage.httpBodyStream, coroutineName: "code-proxy-server")
        /**
         * 代理监听
         * 让远端提供 esm 模块代码
         * 这里我们将请求转发给对方，要求对方以一定的格式提供代码回来，
         * 我们会对回来的代码进行处理，然后再执行
         */
        let codeProxyServerIpc = httpDwebServer?.listen()
        _ = codeProxyServerIpc?.onRequest(cb: { request, ipc in
            // <internal>开头的是特殊路径：交由内部处理，不会推给远端处理
            if request.url != nil, request.url!.path.hasPrefix(self.INTERNAL_PATH) {
                let path = request.url!.path
                let subIndex = path.index(path.startIndex, offsetBy: self.INTERNAL_PATH.count)
                let subPath = String(path[subIndex..<path.endIndex])
                let internalUri = request.url!.replacePath(replacePath: subPath)
                
                let header = IpcHeaders()
                for (key,value) in self.CORS_HEADERS {
                    header.set(key: key, value: value)
                }
                
                if internalUri?.path == "/bootstrap.js" {
                    
                    ipc.postMessage(message: IpcResponse.fromText(req_id: request.req_id, statusCode: 200, text: self.JS_PROCESS_WORKER_CODE, headers: header, ipc: ipc))
                } else {
                    ipc.postMessage(message: IpcResponse.fromText(req_id: request.req_id, statusCode: 404, text: "// no found \(internalUri?.path ?? "")", headers: header, ipc: ipc))
                }
            } else {
                
                guard let tmpRequest = request.toRequest() else { return }
                guard let res = streamIpc.request(request: tmpRequest) else { return }
                for (key,value) in self.CORS_HEADERS {
                    res.headers.add(name: key, value: value)
                }
                ipc.postResponse(req_id: request.req_id, response: res)
            }
        })
        
        let bootstrap_url = httpDwebServer?.startResult.urlInfo.buildInternalUrl()?.replacePath(replacePath: "\(INTERNAL_PATH)/bootstrap.js")?.addURLQuery(name: "mmid", value: ipc.remote?.mmid)?.addURLQuery(name: "host", value: httpDwebServer?.startResult.urlInfo.host)?.absoluteString
        //创建一个通往 worker 的消息通道
        //TODO
        
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
        
        // 返回自定义的 Response，里头携带我们定义的 ipcStream
        let data = IpcResponse.fetchStreamData(stream: streamIpc.stream)
        let responseBody = Response.Body.init(data: data)
        return Response(status: .ok, body: responseBody)
    }
}
