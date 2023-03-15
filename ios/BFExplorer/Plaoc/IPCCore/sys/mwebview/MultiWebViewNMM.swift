//
//  MultiWebViewNMM.swift
//  BFExplorer
//
//  Created by ui03 on 2023/3/2.
//

import Foundation
import Vapor

class MultiWebViewNMM: NativeMicroModule {
    
    private let dwebServer = HTTPServer()
    static var activityMap: [String: PromiseOut<MutilWebViewViewController>] = [:]
    
    init() {
        super.init(mmid: "mwebview.sys.dweb")
    }
    
    override func _bootstrap() throws {
        
        let app = dwebServer.app
        let group = app.grouped("\(mmid)")
        
        group.on(.GET, "open") { request -> Response in
            let response = self.defineHandler(request: request) { reque in
                if let url = reque.query[String.self, at: "url"] {
                    
                }
                return nil
            }
            return response
        }
        
        group.on(.GET, "close") { request -> Response in
            let response = self.defineHandler(request: request) { reque in
                
            }
            return response
        }
    }
    
    func openMutilWebViewActivity(remoteMmid: String) {
        
    }
    
    func openDwebView(remoteMm: MicroModule,urlString: String) -> String {
        
        let remotemmid = remoteMm.mmid
        return ""
    }
    
    func closeDwebView(remoteMmid: String, webviewId: String) -> Bool {
        return true
    }
}
