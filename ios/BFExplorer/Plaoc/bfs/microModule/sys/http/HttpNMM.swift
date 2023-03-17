//
//  HttpsNMM.swift
//  BFExplorer
//
//  Created by ui08 on 2023/2/8.
//

import Foundation
import Vapor

struct DwebHttpServerOptions {
    var port: Int = 80
    var subdomain: String = ""
}

class HttpNMM: NativeMicroModule {
    var tokenMap: [/* token */String:Gateway] = [:]
    var gatewayMap: [/* host */String:Gateway] = [:]
    
    override init() {
        super.init()
        mmid = "http.sys.dweb"
        
        HttpServer.app.middleware.use(RequestMiddleware(httpNMM: self), at: .end)
    }
    
    struct RequestMiddleware: AsyncMiddleware {
        let httpNMM: HttpNMM
        init(httpNMM: HttpNMM) {
            self.httpNMM = httpNMM
        }
        
        func respond(to request: Request, chainingTo next: AsyncResponder) async throws -> Response {
            var header_host: String? = nil
            var header_x_dweb_host: String? = nil
            var header_user_agent_host: String? = nil
            let query_x_web_host: String? = request.query[String.self, at: "X-Dweb-Host"]

            for (key, value) in request.headers {
                switch key {
                case "Host":
                    header_host = value
                case "X-Dweb-Host":
                    header_x_dweb_host = value
                case "User-Agent":
//                    // iOS 16之后的写法
//                    do {
//                        if let result = try /\sdweb-host\/(.+)\s*/.firstMatch(in: value) {
//                            header_user_agent_host = result.output.1
//                        }
//                    } catch {
//
//                    }
                    let result = value.getMatches(regex: #"\sdweb-host\/(.+)\s*"#)
                    if !result.isEmpty {
                        header_user_agent_host = result[0]
                    }
                default:
                    break
                }
            }
            
            var host = query_x_web_host ?? header_x_dweb_host ?? header_user_agent_host ?? header_host ?? "*"
            
            if !host.contains(":") && host != "*" {
                host += ":\(HttpServer.PORT)"
            }
            
            var response: Response?
            let gateway = httpNMM.gatewayMap[host]
            if gateway != nil {
                response = await gateway!.listener.hookHttpRequest(request: request)
            }


//            // 网关未找到判断
//            let gateway = DnsNMM.shared.httpServerNMM.gatewayMap[host]
//            if gateway == nil && !request.url.path.hasPrefix("/http.sys.dweb") {
//                return await DnsNMM.shared.httpServerNMM.defaultErrorResponse(
//                    req: request,
//                    statusCode: .badGateway,
//                    errorMessage: "Bad Gateway",
//                    detailMessage: "作为网关或者代理工作的服务器尝试执行请求时，从远程服务器接收到了一个无效的响应"
//                )
//            }
//
//            // 未找到路由判断
//            let app = HttpServer.app
//            let routes = app.routes.all
//            if !routes.contains(where: { route in
//                let routePath = "/" + route.path.map { "\($0)" }.joined(separator: "/")
//                if routePath == request.url.path && route.method == request.method {
//                    return true
//                } else {
//                    return false
//                }
//            }) {
//                return await DnsNMM.shared.httpServerNMM.defaultErrorResponse(
//                    req: request,
//                    statusCode: .notFound,
//                    errorMessage: "not found",
//                    detailMessage: "未找到"
//                )
//            }
            
            let vaporResponse = try await next.respond(to: request)

            return response ?? vaporResponse
        }
    }

    /// 网关错误，默认返回
    func defaultErrorResponse(req: Request, statusCode: HTTPResponseStatus, errorMessage: String, detailMessage: String) async -> Response {
        var headerJsonString = ""
        _ = req.headers.map { item in
            headerJsonString += "\(item.name): \(item.value)\n"
        }
        var headers = HTTPHeaders()
        headers.add(name: .contentType, value: "text/html")

        return Response(status: statusCode, headers: headers, body: .init(string: """
            <!DOCTYPE html>
                <html>
                    <head>
                        <meta charset="UTF-8" />
                        <meta http-equiv="X-UA-Compatible" content="IE=edge" />
                        <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                        <title>\(statusCode.code)</title>
                    </head>
                    <body>
                        <h1 style="color:red;margin-top:50px;">[\(statusCode.code)] \(errorMessage)</h1>
                        <blockquote>\(detailMessage)</blockquote>
                        <div>
                          <h2>URL:</h2>
                          <pre>\(req.url)</pre>
                        </div>
                        <div>
                          <h2>METHOD:</h2>
                          <pre>\(req.method)</pre>
                        </div>
                        <div>
                          <h2>HEADERS:</h2>
                          <pre>\(headerJsonString)</pre>
                        </div>
                  </body>
            </html>
        """))
    }
    
    override func _bootstrap(bootstrapContext: BootStrapContext) async throws {
        /// 启动http后端服务
        HttpServer.createServer(22206)
        
        /// 路由处理
        routerHandler()
        
        /// 为 nativeFetch 函数提供支持
        _ = _afterShutdownSignal.listen(nativeFetchAdaptersManager.append { _, request in
            if request.url.scheme == "http" && request.url.host != nil && request.url.host!.hasSuffix(".dweb") {
                return await self.httpHandler(request: Request.new(method: request.method, url: request.url.string))
            } else {
                return nil
            }
        })
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
    private func httpHandler(request: Request) async -> Response {
        var header_host: String? = nil
        var header_x_dweb_host: String? = nil
        var header_user_agent_host: String? = nil
        let query_x_web_host: String? = request.query[String.self, at: "X-Dweb-Host"]

        for (key, value) in request.headers {
            switch key {
            case "Host":
                header_host = value
            case "X-Dweb-Host":
                header_x_dweb_host = value
            case "User-Agent":
//                    // iOS 16之后的写法
//                    do {
//                        if let result = try /\sdweb-host\/(.+)\s*/.firstMatch(in: value) {
//                            header_user_agent_host = result.output.1
//                        }
//                    } catch {
//
//                    }
                let result = value.getMatches(regex: #"\sdweb-host\/(.+)\s*"#)
                if !result.isEmpty {
                    header_user_agent_host = result[0]
                }
            default:
                break
            }
        }
        
        var host = query_x_web_host ?? header_x_dweb_host ?? header_user_agent_host ?? header_host ?? "*"
        
        if !host.contains(":") && host != "*" {
            host += ":\(HttpServer.PORT)"
        }
        
        var response: Response?
        let gateway = self.gatewayMap[host]
        if gateway != nil {
            response = await gateway!.listener.hookHttpRequest(request: request)
        }
        
        return response ?? Response(status: .notFound)
    }
    
    private func routerHandler() {
        let startRouteHandler: RouterHandler = { request, ipc async in
            let port = request.query[Int.self, at: "port"]
            let subdomain = request.query[String.self, at: "subdomain"]
            
            return self.start(
                ipc: ipc!, options: DwebHttpServerOptions(
                    port: port ?? 80,
                    subdomain: subdomain?.decodeURIComponent() ?? ""))
        }
        let listenRouteHandler: RouterHandler = { request, _ async in
            guard let token = request.query[String.self, at: "token"],
                  let routes = request.query[String.self, at: "routes"]
            else {
                return Response(status: .badRequest)
            }
            
            let _routes = JSONParse(routes, of: [Gateway.RouteConfig].self)
                    
            return await self.listen(token: token.decodeURIComponent(), message: request, routes: _routes)
        }
        let closeRouteHandler: RouterHandler = { request, ipc async in
            let port = request.query[Int.self, at: "port"]
            let subdomain = request.query[String.self, at: "subdomain"]
            return await self.close(
                ipc: ipc!,
                options: DwebHttpServerOptions(
                    port: port ?? 80,
                    subdomain: subdomain?.decodeURIComponent() ?? ""))
        }
        apiRouting["\(self.mmid)/start"] = startRouteHandler
        apiRouting["\(self.mmid)/listen"] = listenRouteHandler
        apiRouting["\(self.mmid)/close"] = closeRouteHandler
        
        // 添加路由处理方法到http路由中
        let app = HttpServer.app
        let group = app.grouped("\(mmid)")
        let httpHandler: (Request) async throws -> Response = { request async in
            await self.defineHandler(request: request)
        }
        for pathComponent in ["start", "close"] {
            group.on(.GET, [PathComponent(stringLiteral: pathComponent)], use: httpHandler)
        }
        group.on(.POST, ["listen"], use: httpHandler)
    }
    
    func getServerUrlInfo(ipc: Ipc, options: DwebHttpServerOptions) -> ServerUrlInfo {
        let mmid = ipc.remote.mmid
        let subdomainPrefix = options.subdomain == "" || options.subdomain.hasSuffix(".")
            ? options.subdomain
            : "\(options.subdomain)."
        let port = options.port
        if port <= 0 || port >= 65536 {
            fatalError("invalid dweb http port: \(options.port)")
        }
        
        let host = "\(subdomainPrefix)\(mmid):\(port)"
        let internal_origin = "http://\(host)"
        let public_origin = HttpServer.origin
        
        return ServerUrlInfo(host: host, internal_origin: internal_origin, public_origin: public_origin)
    }
    
    struct ServerUrlInfo: Codable {
        /**
         * 标准host，是一个站点的key，只要站点过来时用某种我们认可的方式（x-host/user-agent）携带了这个信息，那么我们就依次作为进行网关路由
         */
        var host: String
        /**
         * 内部链接，带有特殊的协议头，方便自定义解析器对其进行加工
         */
        var internal_origin: String
        /**
         * 相对公网的链接（这里只是相对标准网络访问，当然目前本地只支持localhost链接，所以这里只是针对webview来使用）
         */
        var public_origin: String
        
        func buildPublicUrl() -> URL {
            URL(string: public_origin)!.appending("X-DWeb-Host", value: host)
        }
        
        func buildInternalUrl() -> URL {
            URL(string: internal_origin)!
        }
    }
    
    struct ServerStartResult: Codable {
        var token: String
        var urlInfo: ServerUrlInfo
    }
    
    /**
     * 监听端口，启动服务
     */
    private func start(ipc: Ipc, options: DwebHttpServerOptions) -> ServerStartResult {
        let serverUrlInfo = getServerUrlInfo(ipc: ipc, options: options)
        
        if gatewayMap.keys.contains(where: { $0 == serverUrlInfo.host }) {
            fatalError("already in listen: \(serverUrlInfo.internal_origin)")
        }
        
        let listener = Gateway.PortListener(ipc: ipc, host: serverUrlInfo.host)
        /// ipc 在关闭的时候，自动释放所有的绑定
        _ = listener.onDestroy {
            _ = ipc.onClose {
                Task {
                    await self.close(ipc: ipc, options: options)
                }
                return .OFF
            }
            
            return nil
        }
        
        let token = generateTokenBase64String(8)
        let gateway = Gateway(listener: listener, urlInfo: serverUrlInfo, token: token)
        gatewayMap[serverUrlInfo.host] = gateway
        tokenMap[token] = gateway
        
        return ServerStartResult(token: token, urlInfo: serverUrlInfo)
    }
    
    /**
     *  绑定流监听
     */
    private func listen(token: String, message: Request, routes: [Gateway.RouteConfig]) async -> Response {
        let gateway = tokenMap[token]
        
        if gateway == nil {
            fatalError("no gateway with token: \(token)")
        }
        
        let streamIpc = ReadableStreamIpc(
            remote: gateway!.listener.ipc.remote,
            role: "http-gateway/\(gateway!.urlInfo.host)")
        
//        var data = Data()
//        let sequential = message.eventLoop.makeSucceededFuture(())
//        message.body.drain {
//            switch $0 {
//            case .buffer(var buffer):
//                let _data = buffer.readData(length: buffer.readableBytes)
//                if _data != nil {
//                    data.append(_data!)
//                }
//                return sequential
//            case .error, .end:
//                return sequential
//            }
//        }
//
//        let stream = InputStream(data: data)
//        await streamIpc.bindIncomeStream(stream: stream)
//        let stream = InputStream(data: buffer!.readData(length: buffer!.readableBytes)!)
        await streamIpc.bindIncomeStream(request: message)
        
        
        
        for routerConfig in routes {
            _ = streamIpc.onClose {
                _ = gateway!.listener.addRouter(config: routerConfig, streamIpc: streamIpc)
                return .OFF
            }
        }
        
//        typealias ResponseBodyStream = (BodyStreamWriter) -> ()
//        var a: ResponseBodyStream = { writer in
//
//        }
//        message.body.drain(a)
        
        
        let response = Response(status: .ok, body: .init(stream: { writer in
            message.body.drain { body in
                switch body {
                case .buffer(let buffer):
                    return writer.write(.buffer(buffer))
                case .error(let error):
                    return writer.write(.error(error))
                case .end:
                    return writer.write(.end)
                }
            }
        }))
        

        return Response(status: .ok, body: .init(stream: { $0.responseStreamWriter(stream: streamIpc.stream!) }))
//        return response
    }
    
    private func close(ipc: Ipc, options: DwebHttpServerOptions) async -> Bool {
        let serverUrlInfo = getServerUrlInfo(ipc: ipc, options: options)
        let gateway = gatewayMap.removeValue(forKey: serverUrlInfo.host)
        
        if gateway != nil {
            tokenMap.removeValue(forKey: gateway!.token)
            await gateway!.listener.destroy()
            return true
        } else {
            return false
        }
    }
}
