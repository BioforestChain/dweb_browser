//
//  JsProcessWebApi.swift
//  BFExplorer
//
//  Created by ui08 on 2023/2/27.
//

import Foundation
import WebKit
import Combine

class JsProcessWebApi: NSObject {
    let webView: DWebView
    private let processIdSubject = PassthroughSubject<Int, Never>()
    private var messageIpcPort1: MessagePortIpc?
    private var cancellable: AnyCancellable?
    private var processIdMmidMap: [Int:String] = [:]
    
    init(webView: DWebView) {
        self.webView = webView
        super.init()
    }
    
    struct ProcessInfo: Codable {
        let process_id: Int
    }
    
    class ProcessHandler {
        let info: ProcessInfo
        var ipc: MessagePortIpc
        
        init(info: ProcessInfo, ipc: MessagePortIpc) {
            self.info = info
            self.ipc = ipc
        }
    }
    
    func createProcess(env_script_url: String, remoteModule: MicroModule) async -> ProcessHandler {
        // 创建namespace
        await self.webView.configuration.userContentController.add(LeadScriptHandle(messageHandle: self),
                                                             contentWorld: .world(name: remoteModule.mmid),
                                                             name: "onmessage")
        await self.webView.configuration.userContentController.add(LeadScriptHandle(messageHandle: self),
                                                             contentWorld: .world(name: remoteModule.mmid),
                                                             name: "processId")
        await self.webView.configuration.userContentController.add(LeadScriptHandle(messageHandle: self),
                                                             contentWorld: .world(name: remoteModule.mmid),
                                                             name: "logging")
        
        
        _ = await self.webView.callAsyncJavaScript("""
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
                self.messageIpcPort1 = MessagePortIpc(port: "\(process_id)", remote: remoteModule, role: .server, type: .port1)
                let processHandler = ProcessHandler(info: .init(process_id: process_id), ipc: .init(port: "\(process_id)", remote: remoteModule, role: .client, type: .port2))
                
                continuation.resume(returning: processHandler)
            })
        }
    }
    
    struct RunProcessMainOptions {
        let main_url: String
    }
    
    func runProcessMain(process_id: Int, options: RunProcessMainOptions) async {
        let mmid = processIdMmidMap[process_id]
        await self.webView.callAsyncJavaScript("runProcessMain(process_id, { main_url: main_url });", arguments: ["process_id": process_id, "main_url": options.main_url], in: nil, in: .world(name: mmid!))
    }
    
    func createIpc(process_id: Int) async {
        let mmid = processIdMmidMap[process_id]
        await self.webView.callAsyncJavaScript("""
            const {port1_ipc, port2_ipc} = new MessageChannel();
            port2_ipc.onmessage = (evt) => {
                window.webkit.messageHandlers.ipcOnMessage.postMessage(JSON.stringify(evt.data));
            }
            await createIpc(process_id, port1_ipc);
        """, arguments: ["process_id": process_id], in: nil, in: .world(name: mmid!))
    }
    
    deinit {
        processIdMmidMap.removeAll()
        cancellable?.cancel()
    }
}

extension JsProcessWebApi: WKScriptMessageHandler {
    func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
        if message.name == "onmessage" {
            print("onmessage")
            if messageIpcPort1 != nil {
                guard let body = message.body as? String else { return }
                let ipcMessage = jsonToIpcMessage(data: body, ipc: messageIpcPort1!)
                if ipcMessage != nil {
                    Task {
                        await messageIpcPort1!.postMessage(message: ipcMessage!)
                    }
                }
            }
        } else if message.name == "processId" {
            print("processId")
            guard let process_id = message.body as? Int else  {
                return
            }
            
            processIdSubject.send(process_id)
        } else if message.name == "logging" {
            print("logging")
            print(message.body)
        } else if message.name == "ipcOnMessage" {
            print("ipcOnMessage")
        }
    }
}
