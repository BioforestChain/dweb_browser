//
//  JsMicroModule.swift
//  BFExplorer
//
//  Created by ui03 on 2023/2/25.
//

import UIKit

//TODO
class JsMicroModule: MicroModule {

    private var metadata: JmmMetadata!
    private var processId: Int?
    private var connectingIpcSet = Set<Ipc>()
    
    init(mmid: String, metadata: JmmMetadata) {
        super.init()
        self.mmid = mmid
        self.metadata = metadata
    }
    
    override func _bootstrap() {
        let pid = Int(arc4random_uniform(1000) + 1)
        processId = pid
        //TODO
        let streaamIpc = ReadableStreamIpc(remote: self, role: .CLIENT)
        streaamIpc.onRequest { (request,ipc) in
            
        }
    }
    
    override func _connect(from: MicroModule) -> Ipc? {
        
        guard processId != nil else { return nil }
        //TODO
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


class JmmMetadata {
    
    private var main_url: String
    
    init(urlString: String) {
        self.main_url = urlString
    }
    
    func getMainUrlString() -> String {
        return self.main_url
    }
    
    func setMainUrl(urlString: String) {
        self.main_url = urlString
    }
}
