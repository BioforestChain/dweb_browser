//
//  JsProcessWebApi.swift
//  BFExplorer
//
//  Created by ui03 on 2023/3/2.
//

import Foundation
import WebKit

class JsProcessWebApi {
    
    private var dWebView: DWebView!
    
    init(dWebView: DWebView) {
        
        self.dWebView = dWebView
    }
    
    func isReady() -> Bool {
        
        return dWebView.evaluateSyncJavascriptCode(script: "typeof createProcess") == "function"
    }
    
    func createProcess(env_script_url: String, remoteModule: MicroModule) {
        //TODO
    }
    
    func runProcessMain(process_id: Int, options: RunProcessMainOptions) {
        //TODO
    }
}


struct IpcProcessInfo {
    
    var process_id: Int
    
    init(process_id: Int) {
        self.process_id = process_id
    }
}


struct ProcessHandler {
    
    var info: IpcProcessInfo
    var ipc: MessagePortIpc
    
    init(info: IpcProcessInfo, ipc: MessagePortIpc) {
        self.info = info
        self.ipc = ipc
    }
}

struct RunProcessMainOptions {
    
    var main_url: String
    
    init(main_url: String) {
        self.main_url = main_url
    }
}
