//
//  JmmNMM.swift
//  BFExplorer
//
//  Created by ui03 on 2023/3/2.
//

import Foundation
import Vapor
import HandyJSON

class JmmNMM: NativeMicroModule {
    
    private var HttpServer = HTTPServer()
    var apps: [String: JsMicroModule] = [:]
    private var installingApps: [String: InstallingAppInfo] = [:]
    
    init() {
        super.init(mmid: "jmm.sys.dweb")
    }
    
    override func _bootstrap() throws {
        
        let app = HttpServer.app
        let group = app.grouped("\(mmid)")
        
        group.on(.GET, "install") { request -> Response in
            let response = self.defineHandler(request: request) { reque in
                let metadataUrl = request.query[String.self, at: "metadata-url"] ?? ""
                let res = self.nativeFetch(urlstring: metadataUrl)
                let jmmMetadata = JSONDeserializer<JmmMetadata>.deserializeFrom(json: res?.body.string)
                
                // TODO 根据 jmmMetadata 打开一个应用信息的界面，用户阅读界面信息后，可以点击"安装"
                self.openJmmMatadataInstallPage(jmmMetadata: jmmMetadata)
                return jmmMetadata
            }
            return response
        }
        
        group.on(.GET, "uninstall") { request -> Response in
            let response = self.defineHandler(req: request) { req, ipc in
                let mmid = request.query[String.self, at: "mmid"] ?? ""
                guard let jmm = self.apps[mmid] else { return false }
                self.openJmmMatadataUninstallPage(jmmMetadata: jmm.metadata)
                return true
            }
            return response
        }
        
        group.on(.GET, "query") { request -> Response in
            let response = self.defineHandler(req: request) { req, ipc in
                let result = AppsQueryResult(installedAppList: self.apps.map({ $1.metadata}), installingAppList: self.installingApps.map { $1} )
                return result
            }
            return response
        }
    }
    
    private func openJmmMatadataInstallPage(jmmMetadata: JmmMetadata?) {
        
    }
    
    private func openJmmMatadataUninstallPage(jmmMetadata: JmmMetadata?) {
       // TODO("Not yet implemented")
    }
    
    override func _shutdown() throws {
        
    }
}


struct InstallingAppInfo {
    
    var progress: Float
    var jmmMetadata: JmmMetadata
    
    init(progress: Float, jmmMetadata: JmmMetadata) {
        self.progress = progress
        self.jmmMetadata = jmmMetadata
    }
}


struct AppsQueryResult {
    
    var installedAppList: [JmmMetadata]
    var installingAppList: [InstallingAppInfo]
    
    init(installedAppList: [JmmMetadata], installingAppList: [InstallingAppInfo]) {
        self.installedAppList = installedAppList
        self.installingAppList = installingAppList
    }
}
