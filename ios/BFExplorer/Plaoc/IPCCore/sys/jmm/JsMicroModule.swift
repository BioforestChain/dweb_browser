//
//  JsMicroModule.swift
//  BFExplorer
//
//  Created by ui03 on 2023/2/25.
//

import UIKit
import Vapor
import HandyJSON

//TODO
class JsMicroModule: MicroModule {

    var metadata: JmmMetadata!
    private var processId: String?
    private var connectingIpcSet = Set<Ipc>()
    
    init(metadata: JmmMetadata) {
        super.init()
        self.mmid = metadata.id
        self.metadata = metadata
    }
    
    static var willInit: Void = {
        _ = connectAdapterManager.append { fromMM, toMM, reason in
            guard let toMM = toMM as? JsMicroModule else { return nil }
            let pid = toMM.processId
            if pid == nil {
                fatalError("JMM:\(toMM.mmid) no ready")
            }
            /**
             * 向js模块发起连接
             */
            guard let url = URL(string: "file://js.sys.dweb/create-ipc")?.addURLQuery(name: "process_id", value: "\(pid!)")?.addURLQuery(name: "mmid", value: fromMM.mmid) else {
                fatalError("JMM:\(toMM.mmid) no ready")
            }
            let portId = toMM.nativeFetch(url: url)?.int() ?? 0
            let originIpc = Native2JsIpc(port_id: portId, remote: toMM)
            toMM.beConnect(ipc: originIpc, reason: reason)
            return ConnectResult(ipcForFromMM: originIpc, ipcForToMM: nil)
        }
    }()
    
    override func _bootstrap(bootstrapContext: BootstrapContext) throws {
        let pid = Tools.arc4randomByteArray(count: 8)
        processId = pid
        
        let streamIpc = ReadableStreamIpc(remote: self, role: "code-server")
        _ = streamIpc.onRequest { request,ipc in
            var resposne: Response?
            if request.uri!.path.hasSuffix("/") {
                resposne = Response(status: .forbidden)
            } else {
                resposne = self.nativeFetch(urlstring: (self.metadata.server?.root ?? "") + request.uri!.path)
            }
            if let resp = IpcResponse.fromResponse(req_id: request.req_id, response: resposne!, ipc: ipc) {
                ipc.postMessage(message: resp)
            }
        }
        
        guard let url = URL(string: "file://js.sys.dweb/create-process")?.addURLQuery(name: "entry", value: self.metadata.server?.entry)?.addURLQuery(name: "process_id", value: pid) else { return }
        
        let buffer = ByteBuffer(data: Data(streamIpc.stream.readByteArray()))
        let request = Request.new(method: .POST, url: url.absoluteString, collectedBody: buffer)
        let response = nativeFetch(request: request)
        streamIpc.bindIncomeStream(stream: response?.stream(), coroutineName: streamIpc.role ?? "")
        connectingIpcSet.insert(streamIpc)
        
        //拿到与js.sys.dweb模块的直连通道，它会将 Worker 中的数据带出来
        let jsIpc = bootstrapContext.dns.connect(mmid: "js.sys.dweb", reason: nil)?.ipcForFromMM
        /**
         * 这里 jmm 的对于 request 的默认处理方式是将这些请求直接代理转发出去
         * TODO 跟 dns 要 jmmMetadata 信息然后进行路由限制 eg: jmmMetadata.permissions.contains(ipcRequest.uri.host) // ["camera.sys.dweb"]
         */
        
        _ = jsIpc?.onRequest(cb: { ipcRequest, ipc in
            let request = ipcRequest.toRequest()
            let response = self.nativeFetch(request: request)
            if response != nil {
                let ipcResponse = IpcResponse.fromResponse(req_id: ipcRequest.req_id, response: response!, ipc: ipc)
            } else {
                let ipcResponse = IpcResponse.fromText(req_id: ipcRequest.req_id, text: "bad request", ipc: ipc)
            }
        })
        //收到 Worker 的事件，如果是指令，执行一些特定的操作
        _ = jsIpc?.onEvent(cb: { ipcEvent, ipc in
            //收到要与其它模块进行ipc连接的指令
            if ipcEvent.name == "dns/connect" {
                
                struct DnsConnectEvent: HandyJSON {
                    var mmid: String = ""
                    
                    init() {
                        
                    }
                    
                    init(mmid: String) {
                        self.mmid = mmid
                    }
                }
                
                let event = JSONDeserializer<DnsConnectEvent>.deserializeFrom(json: ipcEvent.text)
                /**
                 * 模块之间的ipc是单例模式，所以我们必须拿到这个单例，再去做消息转发
                 * 但可以优化的点在于：TODO 我们应该将两个连接的协议进行交集，得到最小通讯协议，然后两个通道就能直接通讯raw数据，而不需要在转发的时候再进行一次编码解码
                 *
                 * 此外这里允许js多次建立ipc连接，因为可能存在多个js线程，它们是共享这个单例ipc的
                 */
                /**
                 * 向目标模块发起连接
                 */
                let targetIpc = bootstrapContext.dns.connect(mmid: event?.mmid ?? "", reason: nil)?.ipcForFromMM
                //向js模块发起连接
                if let url = URL(string: "file://js.sys.dweb/create-ipc")?.addURLQuery(name: "process_id", value: pid)?.addURLQuery(name: "mmid", value: event?.mmid) {
                    let portId = self.nativeFetch(url: url)?.int() ?? 0
                    let originIpc = Native2JsIpc(port_id: portId, remote: self)
                    self.beConnect(ipc: originIpc, reason: Request.new(method: .GET, url: "file://$mmid/event/dns/connect"))
                    /**
                     * 将两个消息通道间接互联
                     */
                    _ = originIpc.onMessage { ipcMessage, _ in
                        targetIpc?.postMessage(message: ipcMessage)
                    }
                    _ = targetIpc?.onMessage(cb: { ipcMessage, _ in
                        originIpc.postMessage(message: ipcMessage)
                    })
                }
            } else {
                
            }
        })
        print("running!!" + mmid)
        ipcSet.add(streamIpc)
    }
    
    override func _shutdown() throws {
        /// TODO 发送指令，关停js进程
        processId = nil
    }
}




