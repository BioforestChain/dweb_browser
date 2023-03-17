//
//  JsMicroModule.swift
//  BFExplorer
//
//  Created by kingsword09 on 2023/1/31.
//

import Foundation
import Vapor

/** 可动态加载的微组件 */
class JsMicroModule: MicroModule {
    var metadata: JmmMetadata
    var processId: String? = nil
    
    static var willInit: Void = {
        _ = connectAdapterManager.append { (fromMM, toMM, reason) in
            if let toMM = toMM as? JsMicroModule {
                let pid = toMM.processId
                
                if pid == nil {
                    fatalError("JMM:\(toMM.mmid) no ready")
                }
                
                /**
                 * 向js模块发起连接
                 */
                let portId = await toMM.nativeFetch(url: URL(string: "file://js.sys.dweb/create-ipc")!
                    .appending("process_id", value: pid)
                    .appending("mmid", value: fromMM.mmid).path)
                    .int()!
                
                let originIpc = Native2JsIpc(port_id: portId, remote: toMM, role: .client)
                await toMM.beConnect(ipc: originIpc, reason: reason)
                
                return ConnectResult(ipcForFromMM: originIpc, ipcForToMM: nil)
            } else {
                return nil
            }
        }
    }()
    
    init(metadata: JmmMetadata) {
        _ = JsMicroModule.willInit
        self.metadata = metadata
        super.init()
        self.mmid = metadata.id
    }
    
    override func _bootstrap(bootstrapContext: BootStrapContext) async throws {
        print("bootstrap... \(mmid)/\(metadata)")
        let pid = generateTokenBase64String(8)
        processId = pid
        let streamIpc = ReadableStreamIpc(remote: self, role: "code-server")
        _ = streamIpc.onRequest { (ipcReqMessage, ipc) in
            let ipcRequest = ipcReqMessage.toIpcRequest(ipc: ipc)
            
            var response: Response
            if ipcRequest.uri!.path.hasSuffix("/") {
                response = Response(status: .forbidden)
            } else {
                response = await self.nativeFetch(url: self.metadata.server.root + ipcRequest.uri!.path)
            }
            
            await ipc.postMessage(message: IpcResponse.fromResponse(
                req_id: ipcRequest.req_id,
                response: response,
                ipc: ipc).ipcResMessage)
            
            return nil
        }
        
        let response = await nativeFetch(request: Request.new(
            method: .POST,
            url: URL(string: "file://js.sys.dweb/create-process")!
                .appending("entry", value: metadata.server.entry)
                .appending("process_id", value: pid)
                .absoluteString))
        var buffer = try await response.body.collect(on: HttpServer.app.eventLoopGroup.next()).get()
        
        await streamIpc.bindIncomeStream(
            data: buffer!.readableBytes > 0 ? buffer!.readData(length: buffer!.readableBytes)! : nil)
        
        /**
         * 拿到与js.sys.dweb模块的直连通道，它会将 Worker 中的数据带出来
         */
        let connectResult = await bootstrapContext.dns.connect(mmid: "js.sys.dweb", reason: nil)
        let jsIpc = connectResult.ipcForFromMM
        /**
         * 这里 jmm 的对于 request 的默认处理方式是将这些请求直接代理转发出去
         * TODO 跟 dns 要 jmmMetadata 信息然后进行路由限制 eg: jmmMetadata.permissions.contains(ipcRequest.uri.host) // ["camera.sys.dweb"]
         */
        _ = jsIpc.onRequest { (ipcReqMessage, ipc) in
            let request = ipcReqMessage.toIpcRequest(ipc: ipc).toRequest()
            
            let response = await self.nativeFetch(request: request)
            let ipcResMessage = IpcResponse.fromResponse(req_id: ipcReqMessage.req_id, response: response, ipc: ipc).ipcResMessage
            await ipc.postMessage(message: ipcResMessage)
            
            /*
             await ipc.postMessage(message: IpcResponse.fromText(req_id: ipcReqMessage.req_id, statusCode:500, text: error.localizedDescription ?? "", ipc: ipc))
             */
            
            return nil
        }
        
        /**
         * 收到 Worker 的事件，如果是指令，执行一些特定的操作
         */
        _ = jsIpc.onEvent { ipcEvent, _ in
            /**
             * 收到要与其它模块进行ipc连接的指令
             */
            if ipcEvent.name == "dns/connect" {
                struct DnsConnectEvent: Codable {
                    let mmid: Mmid
                }
                
                do {
                    let event = try JSONDecoder().decode(DnsConnectEvent.self, from: ipcEvent.text.utf8Data()!)
                    /**
                     * 模块之间的ipc是单例模式，所以我们必须拿到这个单例，再去做消息转发
                     * 但可以优化的点在于：TODO 我们应该将两个连接的协议进行交集，得到最小通讯协议，然后两个通道就能直接通讯raw数据，而不需要在转发的时候再进行一次编码解码
                     *
                     * 此外这里允许js多次建立ipc连接，因为可能存在多个js线程，它们是共享这个单例ipc的
                     */
                    /**
                     * 向目标模块发起连接
                     */
                    let connectResult = await bootstrapContext.dns.connect(mmid: event.mmid, reason: nil)
                    let targetIpc = connectResult.ipcForFromMM
                    
                    /**
                     * 向js模块发起连接
                     */
                    let portId = await self.nativeFetch(url: URL(string: "file://js.sys.dweb/create-ipc")!
                        .appending("process_id", value: pid)
                        .appending("mid", value: event.mmid).path)
                    .int()!
                    
                    let originIpc = Native2JsIpc(port_id: portId, remote: self)
                    await self.beConnect(
                        ipc: originIpc,
                        reason: Request.new(method: .GET, url: "file://\(self.mmid)/event/dns/connect"))
                    
                    /**
                     * 将两个消息通道间接互联
                     */
                    _ = originIpc.onMessage { ipcMessage, _ in
                        await targetIpc.postMessage(message: ipcMessage)
                    }
                    _ = targetIpc.onMessage { ipcMessage, _ in
                        await originIpc.postMessage(message: ipcMessage)
                    }
                } catch {
                    print("event text json decode error: \(error.localizedDescription)")
                    return nil
                }
            }
            
            return nil
        }
        
        print("running!!", mmid)
        _ipcSet.insert(streamIpc)
    }
    
    override func _shutdown() async throws {

        processId = nil
    }
}

