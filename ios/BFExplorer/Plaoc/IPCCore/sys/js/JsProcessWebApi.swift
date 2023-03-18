//
//  JsProcessWebApi.swift
//  BFExplorer
//
//  Created by ui03 on 2023/3/2.
//

import Foundation
import WebKit
import Combine

class JsProcessWebApi: NSObject {
    
    var dWebView: DWebView!
    
    private var cancellable: AnyCancellable?
    private let processIdSubject = PassthroughSubject<Int, Never>()
    private var processIdMmidMap: [Int:String] = [:]
    private var messageIpcPort1: MessagePortIpc?
    private var createIpcPort1: WebMessagePort?
    
    init(dWebView: DWebView) {
        
        self.dWebView = dWebView
    }
    
    func isReady() -> Bool {
        
        return dWebView.evaluateSyncJavascriptCode(script: "typeof createProcess") == "function"
    }
    
    func createProcess(env_script_url: String, metadata_json: String, env_json: String, remoteModule: MicroModuleInfo, host: String) async -> ProcessHandler {
        await self.dWebView.configuration.userContentController.add(LeadScriptHandle(messageHandle: self),
                                                                    contentWorld: .world(name: remoteModule.mmid),
                                                                    name: "onmessage")
        await self.dWebView.configuration.userContentController.add(LeadScriptHandle(messageHandle: self),
                                                                    contentWorld: .world(name: remoteModule.mmid),
                                                                    name: "processId")
        await self.dWebView.configuration.userContentController.add(LeadScriptHandle(messageHandle: self),
                                                                    contentWorld: .world(name: remoteModule.mmid),
                                                                    name: "logging")
        
        _ = await self.dWebView.callAsyncJavaScript("""
            const {port1, port2} = new MessageChannel();
            port2.onmessage = (evt) => {
                window.webkit.messageHandlers.onmessage.postMessage(evt.data);
            }
            await new Promise((resolve)=>{self.createProcess_start = resolve});
            try {
                let { process_id } = await createProcess(env_script_url, port1);
                window.webkit.messageHandlers.processId.postMessage(`${process_id}`);
            } catch(err) {
                window.webkit.messageHandlers.logging.postMessage(err.message ?? JSON.stringify(err));
            }
        """.trimmingCharacters(in: .whitespacesAndNewlines), arguments: ["env_script_url":env_script_url], in: nil, in: .world(name: remoteModule.mmid))
        
        return await withCheckedContinuation { continuation in
            cancellable = processIdSubject.sink(receiveValue: { process_id in
                self.processIdMmidMap[process_id] = remoteModule.mmid
                self.messageIpcPort1 = MessagePortIpc(port: WebMessagePort(name: "\(process_id)", role: .port1), remote: remoteModule, role: .SERVER)
                let processHandler = ProcessHandler(info: IpcProcessInfo(process_id: process_id), ipc: MessagePortIpc(port: MessagePort(port: WebMessagePort(name: "\(process_id)", role: .port2)), remote: remoteModule))
                
                continuation.resume(returning: processHandler)
            })
        }
    }
    func runProcessMain(process_id: Int, options: RunProcessMainOptions) {
        let mmid = processIdMmidMap[process_id]
        self.dWebView.callAsyncJavaScript("runProcessMain(process_id, { main_url: main_url });", arguments: ["process_id": process_id, "main_url": options.main_url], in: nil, in: .world(name: mmid!))
    }
    
    func createIpc(process_id: Int, mmid: String) -> Int {
        self.processIdMmidMap[process_id] = mmid
        self.createIpcPort1 = WebMessagePort(name: "createIpc_\(process_id)", role: .port1)
        let port2 = WebMessagePort(name: "createIpc_\(process_id)", role: .port2)
        self.dWebView.callAsyncJavaScript("""
            const {port1_ipc, port2_ipc} = new MessageChannel();
            port2_ipc.onmessage = (evt) => {
                window.webkit.messageHandlers.ipcOnMessage.postMessage(JSON.stringify(evt.data));
            }
            await createIpc(process_id, mmid, port1_ipc);
        """, arguments: ["process_id": process_id, "mmid":mmid], in: nil, in: .world(name: mmid))
        
        return saveNative2JsIpcPort(port: port2)
    }
    
    func destroy() {
        dWebView.removeFromSuperview()
    }
    
}

extension JsProcessWebApi: WKScriptMessageHandler {
    
    func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
        
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
