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
    
    override func _bootstrap(bootstrapContext: BootStrapContext) async throws {
        routerHandler(bootstrapContext: bootstrapContext)
        
        for app in apps.values {
            bootstrapContext.dns.install(mm: app)
        }
    }
    
    private func routerHandler(bootstrapContext: BootStrapContext) {
        let installRouteHandler: RouterHandler = { request, ipc async in
            let metadataUrl = request.query[String.self, at: "metadata-url"]
            
            let jmmMetadata = await self.nativeFetch(url: metadataUrl!.decodeURIComponent()).json(JmmMetadata.self)
            self.openJmmMetadataInstallPage(jmmMetadata: jmmMetadata) { metadata in
                let jmm = JsMicroModule(metadata: metadata)
                self.apps[jmmMetadata.id] = jmm
                bootstrapContext.dns.install(mm: jmm)
            }
            
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
        let downloadRouteHandler: RouterHandler = { request, ipc async in
            let mmid = request.query[Mmid.self, at: "mmid"]
            let jmm = self.apps[mmid!]
            if self.apps[mmid!] == nil {
                let jmm = JsMicroModule(
                    metadata: JmmMetadata(
                        id: mmid!,
                        server: JmmMetadata.MainServer(
                            root: "file://bundle",
                            entry: "/cot.worker.js")))
                
                self.apps[mmid!] = jmm
            }
            
            return true
        }
        apiRouting["\(self.mmid)/install"] = installRouteHandler
        apiRouting["\(self.mmid)/uninstall"] = uninstallRouteHandler
        apiRouting["\(self.mmid)/query"] = queryRouteHandler
        apiRouting["\(self.mmid)/download"] = downloadRouteHandler
        
        // 添加路由处理方法到http路由中
        let app = HttpServer.app
        let group = app.grouped("\(mmid)")
        let httpHandler: (Request) async throws -> Response = { request async in
            await self.defineHandler(request: request)
        }
        for pathComponent in ["install", "uninstall", "query", "download"] {
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
    
    private func openJmmMetadataInstallPage(jmmMetadata: JmmMetadata, installDns: Callback<JmmMetadata, Void>) {
        // TODO: 下载解压压缩包
        // TODO: 使用 file://dns.sys.dweb/install 进行注册
        
    }
    
    private func openJmmMetadataUninstallPage(jmmMetadata: JmmMetadata) {
        // TODO: 未实现
    }
    
    
}
