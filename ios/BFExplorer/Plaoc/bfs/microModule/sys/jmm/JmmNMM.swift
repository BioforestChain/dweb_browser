//
//  JmmNMM.swift
//  BFExplorer
//
//  Created by ui08 on 2023/3/6.
//

import Foundation
import Vapor

class JmmNMM: NativeMicroModule {
    var apps: [Mmid: JsMicroModule] = [:]
    
    override init() {
        super.init()
        mmid = "jmm.sys.dweb"
    }
    
    override func _bootstrap() async throws {
        let app = HttpServer.app
        
        let group = app.grouped("\(mmid)")
        group.on(.GET, ["install"]) { request, ipc in
            let metadataUrl = request.query[String.self, at: "metadata-url"]
            
            let jmmMetadata = await self.nativeFetch(url: metadataUrl!).json(JmmMetadata.self)
            self.openJmmMetadataInstallPage(jmmMetadata: jmmMetadata)
            return Response(status: .ok)
        }
        _ = group.on(.GET, ["uninstall"]) { request, ipc in
            let mmid = request.query[Mmid.self, at: "mmid"]
            let jmm = self.apps[mmid!]
            if jmm == nil {
                fatalError("")
            }
            self.openJmmMetadataUninstallPage(jmmMetadata: jmm!.metadata)
            return Response(status: .ok)
        }
        _ = group.on(.GET, ["query"]) { request, ipc in
            _ = AppQueryResult(installedAppList: self.apps.map { $1.metadata }, installingAppList: self.installingApps.map { $1 })
            return Response(status: .ok)
        }
    }
    
    struct AppQueryResult {
        let installedAppList: [JmmMetadata]
        let installingAppList: [InstallingAppInfo]
    }
    
    struct InstallingAppInfo {
        var progress: Float
        let jmmMetadata: JmmMetadata
    }
    
    private var installingApps: [Mmid: InstallingAppInfo] = [:]
    
    private func openJmmMetadataInstallPage(jmmMetadata: JmmMetadata) {
        // TODO: 下载解压压缩包
        // TODO: 使用 file://dns.sys.dweb/install 进行注册
        
    }
    
    private func openJmmMetadataUninstallPage(jmmMetadata: JmmMetadata) {
        // TODO: 未实现
    }
    
    
}
