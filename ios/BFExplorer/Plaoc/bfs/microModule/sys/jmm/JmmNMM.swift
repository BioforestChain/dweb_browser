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
        routerHandler()
    }
    
    private func routerHandler() {
        let installRouteHandler: RouterHandler = { request, ipc async in
            let metadataUrl = request.query[String.self, at: "metadata-url"]
            
            let jmmMetadata = await self.nativeFetch(url: metadataUrl!).json(JmmMetadata.self)
            self.openJmmMetadataInstallPage(jmmMetadata: jmmMetadata)
            
            return jmmMetadata
        }
        let uninstallRouteHandler: RouterHandler = { request, ipc async in
            let mmid = request.query[Mmid.self, at: "mmid"]
            let jmm = self.apps[mmid!]
            if jmm == nil {
                fatalError("")
            }
            self.openJmmMetadataUninstallPage(jmmMetadata: jmm!.metadata)
            return true
        }
        let queryRouteHandler: RouterHandler = { request, ipc async in
            return AppQueryResult(installedAppList: self.apps.map { $1.metadata }, installingAppList: self.installingApps.map { $1 })
        }
        apiRouting["\(self.mmid)/install"] = installRouteHandler
        apiRouting["\(self.mmid)/uninstall"] = uninstallRouteHandler
        apiRouting["\(self.mmid)/query"] = queryRouteHandler
        
        // 添加路由处理方法到http路由中
        let app = HttpServer.app
        let group = app.grouped("\(mmid)")
        let httpHandler: (Request) async throws -> Response = { request async in
            await self.defineHandler(request: request)
        }
        for pathComponent in ["install", "uninstall", "query"] {
            group.on(.GET, [PathComponent(stringLiteral: pathComponent)], use: httpHandler)
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
