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
    
    static var apps: [String: JsMicroModule] = [:]
    private var installingApps: [String: InstallingAppInfo] = [:]
    
    static let hostName = "file://jmm.sys.dweb"
    static var jmmNMM: JmmNMM?
    
    init() {
        super.init(mmid: "jmm.sys.dweb")
        
        // TODO 启动的时候，从数据库中恢复 apps 对象
        Task {
            
        }
        
        JmmNMM.jmmNMM = self
    }
    
    static func getAndUpdateJmmNmmApps() -> [String: JsMicroModule] {
        return apps
    }
    
    static func nativeFetchFromJS(mmid: String) {
        Task {
            let response = self.apps[mmid]?.nativeFetch(urlstring: "file://dns.sys.dweb/open?app_id=\(mmid.encodeURIComponent())")
            if response == nil {
                print("JmmNMM no found jmm mmid \(mmid)")
            }
        }
    }
    
    static func nativeFetchOpenInstall(jmmMetadata: JmmMetadata, url: String) {
        Task {
            jmmNMM?.nativeFetch(urlstring: "\(hostName)/open?mmid=\(jmmMetadata.id)&metadataUrl=\(url)")
        }
    }
    
    static func nativeFetchInstallDNS(jmmMetadata: JmmMetadata) {
        Task {
            jmmNMM?.nativeFetch(urlstring: "\(hostName)/install?metadata=\(jmmMetadata.toJSONString() ?? "")")
        }
    }
    
    override func _bootstrap(bootstrapContext: BootstrapContext) throws {
        
        for app in JmmNMM.apps.values {
            bootstrapContext.dns.install(mm: app)
        }
        routerHandler(bootstrapContext: bootstrapContext)
    }
    
    private func routerHandler(bootstrapContext: BootstrapContext) {
        let installRouteHandler: RouterHandler = { request, ipc in
            
            let metadata = request.query[String.self, at: "metadata"] ?? ""
            guard let jmmMetadata = JSONDeserializer<JmmMetadata>.deserializeFrom(json: metadata) else {
                return false
            }
            
            var module = JmmNMM.apps[jmmMetadata.id]
            if module == nil {
                module = JsMicroModule(metadata: jmmMetadata)
                bootstrapContext.dns.install(mm: module!)
                JmmNMM.apps[jmmMetadata.id] = module
            }
            return true
        }
        let uninstallRouteHandler: RouterHandler = { request, ipc in
            let mmid = request.query[String.self, at: "mmid"] ?? ""
            let jmm = JmmNMM.apps[mmid]
            if jmm == nil {
                fatalError("")
            }
            self.openJmmMatadataUninstallPage(jmmMetadata: jmm!.metadata)
            return true
        }
        
        let queryRouteHandler: RouterHandler = { request, ipc in
            return AppsQueryResult(installedAppList: JmmNMM.apps.map { $1.metadata }, installingAppList: self.installingApps.map { $1 })
        }
        
        let openRouteHandler: RouterHandler = { request, ipc in
            let metadataUrl = request.query[String.self, at: "metadataUrl"] ?? ""
            let jmmMetadata = JSONDeserializer<JmmMetadata>.deserializeFrom(json: self.nativeFetch(urlstring: metadataUrl)?.body.string ?? "")
            self.openJmmMatadataInstallPage(jmmMetadata: jmmMetadata)
            return jmmMetadata
        }
        apiRouting["\(self.mmid)/install"] = installRouteHandler
        apiRouting["\(self.mmid)/uninstall"] = uninstallRouteHandler
        apiRouting["\(self.mmid)/query"] = queryRouteHandler
        apiRouting["\(self.mmid)/open"] = openRouteHandler
        
        // 添加路由处理方法到http路由中
        let app = HttpServer.app
        let group = app.grouped("\(mmid)")
        let httpHandler: (Request) throws -> Response = { request in
            self.defineHandler(request: request)
        }
        for pathComponent in ["install", "uninstall", "query", "open"] {
            group.on(.GET, [PathComponent(stringLiteral: pathComponent)], use: httpHandler)
        }
    }
    
    private func openJmmMatadataInstallPage(jmmMetadata: JmmMetadata?) {
        //TODO
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
