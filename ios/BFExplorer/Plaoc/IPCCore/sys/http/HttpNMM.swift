//
//  HttpNMM.swift
//  BFExplorer
//
//  Created by ui03 on 2023/3/2.
//

import Foundation
import Vapor

class HttpNMM: NativeMicroModule {
    
    private var tokenMap: [String:Gateway] = [:]
    private var gatewayMap: [String:Gateway] = [:]
    
    init() {
        super.init(mmid: "http.sys.dweb")
    }
    
    /**
        * 监听请求
        *
        * 真实过来的请求有两种情况：
        * 1. http://subdomain.localhost:24433
        * 2. http://localhost:24433
        * 前者是桌面端自身 chrome 支持的情况，后者才是常态。
        * 但是我们返回给开发者的端口只有一个，这就意味着我们需要额外手段进行路由
        *
        * 如果这个请求是发生在 nativeFetch 中，我们会将请求的 url 改成 http://localhost:24433，同时在 headers.user-agent 的尾部加上 dweb-host/subdomain.localhost:24433
        * 如果这个请求是 webview 中发出，我们一开始就会为整个 webview 设置 user-agent，使其行为和上条一致
        *
        * 如果在 webview 中，要跨域其它请求，那么 webview 的拦截器能对 get 请求进行简单的转译处理，
        * 否则其它情况下，需要开发者自己用 fetch 接口来发起请求。
        * 这些自定义操作，都需要在 header 中加入 X-Dweb-Host 字段来指明宿主
        */
    private func httpHandler(_ request: Request) -> Response? {
        var header_host: String?
        var header_x_dweb_host: String?
        var header_user_agent_host: String?
        let query_x_web_host = request.query[String.self, at: "X-DWeb-Host"]
        
        for (key,value) in request.headers {
            switch key {
            case "Host":
                header_host = value
            case "X-Dweb-Host":
                header_x_dweb_host = value
            case "User-Agent":
                let result = value.getMatches(regex: #"\sdweb-host\/(.+)\s*"#)
                if result.count > 0 {
                    header_user_agent_host = result[0]
                }
            default:
                break
            }
        }
        
        var host = query_x_web_host ?? header_x_dweb_host ?? header_user_agent_host ?? header_host
        if host == nil {
            host = "*"
        } else {
            if !host!.contains(":") {
                host = host! + ":" + "\(HTTPServer.PORT)"
            }
        }
        
        var response: Response?
        let gateway = gatewayMap[host!]
        if gateway != nil {
            response = gateway?.listener.hookHttpRequest(request: request)
        }
        if response == nil {
            response = Response(status: .notFound)
        }
        
        return response
    }
    
    override func _bootstrap(bootstrapContext: BootstrapContext) throws {
        HTTPServer.createServer(22605)
        
        // 路由处理
        routerHandler()
        // 为 nativeFetch 函数提供支持
        
        _ = afterShutdownSignal.listen({ _ in
            let off = nativeFetchAdaptersManager.append { (remote, request) -> Response? in
                if (request.url.scheme == "http" || request.url.scheme == "https"), ((request.url.host?.hasSuffix(".dweb")) != nil) {
                    // 无需走网络层，直接内部处理掉
                    guard let url = URL(string: request.url.string) else { return nil }
                    request.headers.add(name: "X-Dweb-Host", value: url.getFullAuthority())
                    request.url.scheme = "http"
                    let content = request.url.string.regexReplacePattern(pattern: url.authority(), replaceString: HTTPServer.authority)
                    request.url = URI(string: content)
                    return self.httpHandler(request)
                } else {
                    return nil
                }
            }
            return off
        })
    }
    
    private func routerHandler() {
        let startRouteHandler: RouterHandler = { request, ipc in
            let port = request.query[Int.self, at: "port"]
            let subdomain = request.query[String.self]
            
            return self.start(ipc: ipc!, options: DwebHttpServerOptions(port: port ?? 80, subdomain: subdomain ?? ""))
        }
        let listenRouteHandler: RouterHandler = { request, _ in
            let token = request.query[String.self, at: "token"] ?? ""
            let routes = request.query[String.self, at: "routes"] ?? ""
            guard let type_routes = ChangeTools.jsonArrayToModel(jsonStr: routes, RouteConfig.self) as? [RouteConfig] else {
                return self.badResponse(request: request)
            }
            return self.listen(token: token, message: request, routes: type_routes) ?? self.badResponse(request: request)
        }
        let closeRouteHandler: RouterHandler = { request, ipc in
            let port = request.query[Int.self, at: "port"]
            let subdomain = request.query[String.self]
            return self.close(ipc: ipc!, options: DwebHttpServerOptions(port: port ?? 80, subdomain: subdomain ?? ""))
        }
        apiRouting["\(self.mmid)/start"] = startRouteHandler
        apiRouting["\(self.mmid)/listen"] = listenRouteHandler
        apiRouting["\(self.mmid)/close"] = closeRouteHandler
        
        // 添加路由处理方法到http路由中
        let app = HTTPServer.app
        let group = app.grouped("\(mmid)")
        let httpHandler: (Request) async throws -> Response = { request async in
            await self.defineHandler(request: request)
        }
        for pathComponent in ["start", "listen", "close"] {
            group.on(.GET, [PathComponent(stringLiteral: pathComponent)], use: httpHandler)
        }
    }
    
    func getServerUrlInfo(ipc: Ipc, options: DwebHttpServerOptions) -> ServerUrlInfo? {
        
        let mmid = ipc.remote?.mmid ?? ""
        var subdomainPrefix = ""
        if options.subdomain == "" || options.subdomain.hasSuffix(".") {
            subdomainPrefix = options.subdomain
        } else {
            subdomainPrefix = options.subdomain + "."
        }
        
        var port: Int
        if options.port <= 0 || options.port >= 65536 {
            return nil
        } else {
            port = options.port
        }
        
        let host = "\(subdomainPrefix)\(mmid):\(port)"
        let internal_origin = "http://\(host)"
        let public_origin = HTTPServer.origin
        return ServerUrlInfo(host: host, internal_origin: internal_origin, public_origin: public_origin)
    }
    
    override func _shutdown() throws {
        HTTPServer.shutdown()
    }
    
    private func start(ipc: Ipc, options: DwebHttpServerOptions) -> ServerStartResult? {
        
        guard let serverUrlInfo = getServerUrlInfo(ipc: ipc, options: options) else { return nil }
        guard !gatewayMap.keys.contains(serverUrlInfo.host) else { return nil }
        
        let portListener = PortListener(ipc: ipc, host: serverUrlInfo.host)
        _ = portListener.onDestroy { _ in
            ipc.onClose { _ in
                self.close(ipc: ipc, options: options)
            }
        }
        
        let token = encoding.generateTokenBase64String(64)
        let gateway = Gateway(listener: portListener, urlInfo: serverUrlInfo, token: token)
        gatewayMap[serverUrlInfo.host] = gateway
        tokenMap[token] = gateway
        
        // http://user:password@host:port/path
        //let url = URL(string: "http://skdf:sldkfjkf@www.baidu.com:80/index.html")
        
        
        return ServerStartResult(token: token, urlInfo: serverUrlInfo)
        
    }
    
    private func listen(token: String, message: Request, routes: [RouteConfig]) -> Response? {
        
        guard let gateway = tokenMap[token] else { return nil }
        guard let remote = gateway.listener.ipc.remote else { return nil }
        
        let streamIpc = ReadableStreamIpc(remote: remote, role: "http-gateway/\(gateway.urlInfo.host)")
        guard let buffer = message.body.data else { return nil }
        let stream = InputStream(data: Data(buffer: buffer))
        
        streamIpc.bindIncomeStream(stream: stream, coroutineName: streamIpc.role ?? "")
        for config in routes {
            _ = streamIpc.onClose { _ in
                gateway.listener.addRouter(config: config, streamIpc: streamIpc)
            }
        }
        
        return Response(status: .ok, body: Response.Body(data: Data(buffer: buffer)))
    }
    
    private func close(ipc: Ipc, options: DwebHttpServerOptions) -> Bool {
        
        let serverUrlInfo = getServerUrlInfo(ipc: ipc, options: options)
        let gateway = gatewayMap.removeValue(forKey: serverUrlInfo?.host ?? "")
        guard gateway != nil else { return false }
        tokenMap.removeValue(forKey: gateway!.token)
        gateway!.listener.destroy()
        return true
    }
    
    private func badResponse(request: Request) -> Response {
        let content = "the \(request.url.path) file not found."
        let body = Response.Body.init(string: content)
        let response = Response(status: .notFound, headers: HTTPHeaders(), body: body)
        return response
    }
}


