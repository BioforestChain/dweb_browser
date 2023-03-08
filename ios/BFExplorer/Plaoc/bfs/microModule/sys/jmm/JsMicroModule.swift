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
    var processId: Int? = nil
    
    init(metadata: JmmMetadata) {
        self.metadata = metadata
        super.init()
        self.mmid = metadata.id
    }
    
    override func _bootstrap() async throws {
        print("bootstrap... \(mmid)/\(metadata)")
        let pid = Int.random(in: 1...1000)
        processId = pid
        let streamIpc = ReadableStreamIpc(remote: self, role: .client)
        _ = streamIpc.onRequest { (item) in
            let request = item.0
            
            if request.uri?.path == "/index.js" {
                Task {
                    _ = await self.nativeFetch(url: self.metadata.main_url)
                }
            } else {
                _ = Response(status: .notFound)
            }
            
            return nil
        }
        
        let request = Request.new(method: .POST, url: "file://js.sys.dweb/create-process?main_pathname=/index.js&process_id=\(pid)")
        let inputStream = await nativeFetch(request: request).stream()
        
        await streamIpc.bindIncomeStream(stream: inputStream)
        
        print("JS进程创建完成 \(mmid)")
        
        _connectingIpcSet.insert(streamIpc)
    }
    
    override func _connect(from: MicroModule) async -> Ipc {
        let pid = processId
        if pid == nil {
            fatalError("\(mmid) process_id no found, should bootstrap first")
        }
        let request = Request.new(method: .POST, url: "file://js.sys.dweb/create-ipc?process_id=\(pid!)")
        let portId = await nativeFetch(request: request).int()
        
        if portId == nil {
            fatalError("create-ipc process_id return portId not int type")
        }
        
        let outerIpc = Native2JsIpc(port_id: portId!, remote: self)
        _connectingIpcSet.insert(outerIpc)
        return outerIpc
    }
    
    private var _connectingIpcSet: Set<Ipc> = []
    
    override func _shutdown() async throws {
        for outerIpc in _connectingIpcSet {
            await outerIpc.close()
        }
        
        _connectingIpcSet.removeAll()
        
        processId = nil
    }
}

