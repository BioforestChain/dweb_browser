//
//  JsMicroModule.swift
//  BFExplorer
//
//  Created by ui03 on 2023/2/25.
//

import UIKit
import Vapor

//TODO
class JsMicroModule: MicroModule {

    var metadata: JmmMetadata!
    private var processId: Int?
    private var connectingIpcSet = Set<Ipc>()
    
    init(metadata: JmmMetadata) {
        super.init()
        self.mmid = metadata.id
        self.metadata = metadata
    }
    
    override func _bootstrap() {
        let pid = Int(arc4random_uniform(1000) + 1)
        processId = pid
        
        let streamIpc = ReadableStreamIpc(remote: self, role: .CLIENT)
        _ = streamIpc.onRequest { request,ipc in
            var resposne: Response?
            if request.url?.path == "/index.js" {
                resposne = self.nativeFetch(urlstring: self.metadata.downloadUrl)
            } else {
                resposne = Response(status: .notFound)
            }
            if let resp = IpcResponse.fromResponse(req_id: request.req_id, response: resposne!, ipc: ipc) {
                ipc.postMessage(message: resp)
            }
        }
        
        guard let url = URL(string: "file://js.sys.dweb/create-process?main_pathname=/index.js&process_id=\(pid)") else { return }
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        let response = nativeFetch(request: request)
        streamIpc.bindIncomeStream(stream: response?.stream(), coroutineName: "\(pid)")
        connectingIpcSet.insert(streamIpc)
    }
    
    override func _connect(from: MicroModule) -> Ipc? {
        
        guard processId != nil else { return nil }
        let portId = 1
        return nil
    }
    
    override func _shutdown() throws {
        for ipc in connectingIpcSet {
            ipc.closeAction()
        }
        connectingIpcSet.removeAll()
        /// TODO 发送指令，关停js进程
        processId = nil
    }
}




