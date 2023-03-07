//
//  JsProcessWebApi.swift
//  BFExplorer
//
//  Created by ui03 on 2023/3/2.
//

import Foundation
import WebKit

class JsProcessWebApi {
    
    private var wkWebView: WKWebView!
    
    init(dWebView: WKWebView) {
        
        self.wkWebView = dWebView
    }
    
    func isReady() -> Bool {
        
        var ready: Bool = false
        let semaphore = DispatchSemaphore(value: 0)
        wkWebView.evaluateJavaScript("typeof createProcess") { result, error in
            ready = result as? String == "function"
            semaphore.signal()
        }
        semaphore.wait()
        return ready
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
