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
            var query_x_web_host: String? = request.query[String.self, at: "X-Dweb-Host"]

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
            var gateway = httpNMM.gatewayMap[host]
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

            return try await next.respond(to: request)
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
    
    override func _bootstrap() async throws {
        HttpServer.createServer(22206)
        let app = HttpServer.app
        
//        _afterShutdownSignal
        
        let group = app.grouped("\(mmid)")
        group.on(.GET, "start") { request in
            let port = request.query[Int.self, at: "port"]
            let subdomain = request.query[String.self]
            
            _ = self.start(ipc: request.ipc!, options: DwebHttpServerOptions(port: port ?? 80, subdomain: subdomain ?? ""))
            return Response(status: .ok)
        }
        group.on(.POST, "listen") { request in
            guard let token = request.query[String.self, at: "token"] else {
                return Response(status: .badRequest)
            }
                    
            return await self.listen(token: token, message: request)
        }
        group.on(.GET, "close") { request in
            let port = request.query[Int.self, at: "port"]
            let subdomain = request.query[String.self]
            await self.close(ipc: request.ipc!, options: DwebHttpServerOptions(port: port ?? 80, subdomain: subdomain ?? ""))
            return Response(status: .ok)
        }
    }
    
    func getServerUrlInfo(ipc: Ipc, options: DwebHttpServerOptions) -> ServerUrlInfo {
        let mmid = ipc.remote.mmid
        let subdomainPrefix = options.subdomain == "" || options.subdomain.hasSuffix(".") ? options.subdomain : "\(options.subdomain)."
        let port = options.port
        if port <= 1024 || port >= 65536 {
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
    
    private func start(ipc: Ipc, options: DwebHttpServerOptions) -> ServerStartResult {
        let serverUrlInfo = getServerUrlInfo(ipc: ipc, options: options)
        
        if gatewayMap.keys.contains(where: { $0 == serverUrlInfo.host }) {
            fatalError("already in listen: \(serverUrlInfo.internal_origin)")
        }
        
        let listener = Gateway.PortListener(ipc: ipc, host: serverUrlInfo.host)
        _ = listener.onDestroy {
            _ = ipc.onClose {
                Task {
                    await self.close(ipc: ipc, options: options)
                }
                return .OFF
            }
            
            return nil
        }
        
        let token = generateTokenBase64String(64)
        let gateway = Gateway(listener: listener, urlInfo: serverUrlInfo, token: token)
        gatewayMap[serverUrlInfo.host] = gateway
        tokenMap[token] = gateway
        
        return ServerStartResult(token: token, urlInfo: serverUrlInfo)
    }
    
    private func listen(token: String, message: Request) async -> Response {
        let gateway = tokenMap[token]
        
        if gateway == nil {
            fatalError("no gateway with token: \(token)")
        }
        
        let streamIpc = ReadableStreamIpc(remote: gateway!.listener.ipc.remote, role: .server)

        var data = Data()
        var sequential = message.eventLoop.makeSucceededFuture(())
        message.body.drain {
            switch $0 {
            case .buffer(var buffer):
                let _data = buffer.readData(length: buffer.readableBytes)
                if _data != nil {
                    data.append(_data!)
                }
                return sequential
            case .error, .end:
                return sequential
            }
        }
        
        let stream = InputStream(data: data)
        await streamIpc.bindIncomeStream(stream: stream)
        
        return Response(status: .ok, body: .init(stream: { writer in
            let stream = streamIpc.stream
            let bufferSize = 1024
            stream.open()
            
            while stream.hasBytesAvailable {
                var data = Data()
                var buffer = [UInt8](repeating: 0, count: bufferSize)
                let bytesRead = stream.read(&buffer, maxLength: bufferSize)
                if bytesRead < 0 {
                    stream.close()
                    _ = writer.write(.error("Error reading from stream" as! Error))
                } else if bytesRead == 0 {
                    stream.close()
                    _ = writer.write(.end)
                }
                data.append(buffer, count: bytesRead)
                var byteBuffer = ByteBuffer(data: data)
                _ = writer.write(.buffer(byteBuffer))
            }
        }))
    }
    
    private func close(ipc: Ipc, options: DwebHttpServerOptions) async {
        let serverUrlInfo = getServerUrlInfo(ipc: ipc, options: options)
        let gateway = gatewayMap.removeValue(forKey: serverUrlInfo.host)
        
        if gateway != nil {
            tokenMap.removeValue(forKey: gateway!.token)
            await gateway!.listener.destroy()
        }
    }
}

//let customProtocol = "http"
//
//enum REQUEST_METHOD: String {
//    case get = "GET"
//    case post = "POST"
//    case put = "PUT"
//    case delete = "DELETE"
//    case options = "OPTIONS"
//}
//
//struct HttpRequestInfo {
//    var http_req_id: Int
//    var url: String
//    var method: REQUEST_METHOD
//    var rawHeaders: [String]
//}
//
//struct HttpResponseInfo {
//    var http_req_id: Int
//    var statusCode: Int
//    var headers: [String:String]
//    // string | Uint8Array | ReadableStream<Uint8Array | string>
//    var body: Any
//}
//
//class HttpListener {
//    var host: String
//    var port: Int
//
//    struct Streams {
//        let input: InputStream
//        let output: OutputStream
//    }
//
//    init(host: String, port: Int) {
//        self.host = host
//        self.port = port
//    }
//
//    lazy var origin: String = "\(customProtocol)://\(host)"
//
//    var _http_req_id_acc = 0
//    func allocHttpReqId() -> Int {
//        return _http_req_id_acc++
//    }
//
////    func hookHttpRequest()
//}
//
//struct ReqMathcher: Hashable {
//    let pathname: String
//    let matchMode: MatchMode
//    let method: HTTPMethod
//
//    static func ==(lhs: ReqMathcher, rhs: ReqMathcher) -> Bool {
//        return lhs.pathname == rhs.pathname
//    }
//
//    func hash(into hasher: inout Hasher) {
//        hasher.combine(pathname)
//    }
//}
//
////struct Router: Hashable {
////    let routes: [ReqMathcher]
////    var streamIpc: NativeIpc
////
////    func hash(into hasher: inout Hasher) {
////        hasher.combine(streamIpc)
////    }
////
////    static func == (lhs: Router, rhs: Router) -> Bool {
////        return lhs.streamIpc == rhs.streamIpc
////    }
////}
//
//func isMatchReq(matcher: ReqMathcher, pathname: String, method: HTTPMethod) -> Bool {
//    return (
//        (matcher.method ?? HTTPMethod.GET) == method &&
//        (matcher.matchMode == MatchMode.full
//         ? pathname == matcher.pathname
//         : matcher.matchMode == MatchMode.prefix
//         ? pathname.hasPrefix(matcher.pathname)
//         : false)
//    )
//}
//
//class PortListener {
//    let ipc: NativeIpc?
//    let host: String
//    let origin: String
//
//    init(ipc: NativeIpc?, host: String, origin: String) {
//        self.ipc = ipc
//        self.host = host
//        self.origin = origin
//    }
//
//    private var _routers: Set<Router> = []
//    func addRouter(router: Router) -> () -> Void {
//        _routers.insert(router)
//
//        return {
//            self._routers.remove(router)
//            return
//        }
//    }
//
//    private func _isBindMatchReq(pathname: String, method: HTTPMethod) -> (Router, ReqMathcher)? {
//        for bind in _routers {
//            for pathMatcher in bind.routes {
//                if isMatchReq(matcher: pathMatcher, pathname: pathname, method: method) {
//                    return (bind, pathMatcher)
//                }
//            }
//        }
//
//        return nil
//    }
//
//    func hookHttpRequest(req: Request) {
//        let url = req.url.path
//        let method = req.method
//
//        var ipc_req_body_stream: Data = Data()
//
//        if method == .POST || method == .PUT {
//            var sequential = req.eventLoop.makeSucceededFuture(())
//
//            req.body.drain {
//                switch $0 {
//                case .buffer(var buffer):
//                    if buffer.readableBytes > 0 {
//                        ipc_req_body_stream.append(buffer.readData(length: buffer.readableBytes)!)
//                    }
//
//                    return sequential
//                case .error(_):
//                    return sequential
//                case .end:
//                    return sequential
//                }
//            }
//        }
//
////        let filePath = rootFilePath(fileName: fileName) + path
////        guard FileManager.default.fileExists(atPath: filePath) else { return "" }
////        let stream = InputStream(fileAtPath: filePath)
////        stream?.open()
////        defer {
////            stream?.close()
////        }
////
////        let bufferSize = 1024
////        let buffer = UnsafeMutablePointer<UInt8>.allocate(capacity: bufferSize)
////        defer {
////            buffer.deallocate()
////        }
////        var result: String = ""
////        while stream!.hasBytesAvailable {
////            let length = stream!.read(buffer, maxLength: bufferSize)
////            let data = Data(bytes: buffer, count: length)
////            let content = String(data: data, encoding: .utf8) ?? ""
////            result += content
////        }
////        return result
//    }
//}
//
//struct GetHostOptions {
//    var ipc: NativeIpc?
//    var port: Int?
//    var subdomain: String?
//}
//
//class HttpServerNMM: NativeMicroModule {
//    var tokenMap: [/* token */String:PortListener] = [:]
//    var gatewayMap: [/* host */String:PortListener] = [:]
//
//    convenience init() {
//        self.init(mmid: "http.sys.dweb")
//    }
//
//    let port = 22605
//    override func _bootstrap() -> Any {
//        Task(priority: .background) {
//            do {
//                HttpServer.createServer(port)
//                let app = HttpServer.app
//
//                app.get(["\(self.mmid)", "listen"]) { req in
//
//                    guard let port = req.query[Int.self, at: "port"],
//                          let subdomain = req.query[String.self, at: "subdomain"],
//                          let mmid = req.query[String.self, at: "mmid"]
//                    else {
//                        return ""
//                    }
//
//                    let (_, origin) = self.listen(hostOptions: HostParam(port: port, mmid: mmid, subdomain: subdomain))
//                    print(origin)
//
//                    return origin
//                }
//
//                app.get(["\(self.mmid)", "unlisten"]) { req in
//                    guard let port = req.query[Int.self, at: "port"],
//                          let subdomain = req.query[String.self, at: "subdomain"],
//                          let mmid = req.query[String.self, at: "mmid"]
//                    else {
//                        return false
//                    }
//
//                    return self.unlisten(hostOptions: HostParam(port: port, mmid: mmid, subdomain: subdomain))
//                }
//
//                app.middleware.use(RequestMiddleware())
//
//                try app.start()
//            } catch {
//                fatalError("http server start error: \(error)")
//            }
//        }
//    }
//
//    struct RequestMiddleware: AsyncMiddleware {
//        func respond(to request: Request, chainingTo next: AsyncResponder) async throws -> Response {
//            var host = "*"
//
//            if request.headers.contains(name: "User-Agent") {
//                host = request.headers.first(name: "User-Agent")!
//
//                print("RequestMiddleware host: \(host)")
//            }
//
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
//
//            return try await next.respond(to: request)
//        }
//    }
//
//    /// 网关错误，默认返回
//    func defaultErrorResponse(req: Request, statusCode: HTTPResponseStatus, errorMessage: String, detailMessage: String) async -> Response {
//        var headerJsonString = ""
//        _ = req.headers.map { item in
//            headerJsonString += "\(item.name): \(item.value)\n"
//        }
//        var headers = HTTPHeaders()
//        headers.add(name: .contentType, value: "text/html")
//
//        return Response(status: statusCode, headers: headers, body: .init(string: """
//            <!DOCTYPE html>
//                <html>
//                    <head>
//                        <meta charset="UTF-8" />
//                        <meta http-equiv="X-UA-Compatible" content="IE=edge" />
//                        <meta name="viewport" content="width=device-width, initial-scale=1.0" />
//                        <title>\(statusCode.code)</title>
//                    </head>
//                    <body>
//                        <h1 style="color:red;margin-top:50px;">[\(statusCode.code)] \(errorMessage)</h1>
//                        <blockquote>\(detailMessage)</blockquote>
//                        <div>
//                          <h2>URL:</h2>
//                          <pre>\(req.url)</pre>
//                        </div>
//                        <div>
//                          <h2>METHOD:</h2>
//                          <pre>\(req.method)</pre>
//                        </div>
//                        <div>
//                          <h2>HEADERS:</h2>
//                          <pre>\(headerJsonString)</pre>
//                        </div>
//                  </body>
//            </html>
//        """))
//    }
//
//    private func listen(hostOptions: HostParam) -> (String, String) {
//        let host = self.getHost(hostOption: HostParam(port: hostOptions.port, mmid: hostOptions.mmid, subdomain: hostOptions.subdomain))
//        let origin = "\(customProtocol)://\(host)"
//
//        // TODO: 未完成base64加密
//        let token = "dweb-browser-random-token"
//
//        self.gatewayMap[host] = PortListener(ipc: nil, host: host, origin: origin)
//        return (token, origin)
//    }
//
//    private func unlisten(hostOptions: HostParam) -> Bool {
//        let host = self.getHost(hostOption: HostParam(port: hostOptions.port, mmid: hostOptions.mmid, subdomain: hostOptions.subdomain))
//
//        let gateway = self.gatewayMap[host]
//
//        if gateway == nil {
//            return false
//        }
//
//        self.tokenMap.removeValue(forKey: host)
//        self.gatewayMap.removeValue(forKey: host)
//
//        return true
//    }
//
//    struct HostParam {
//        var port: Int
//        var mmid: MMID
//        var subdomain: String
//    }
//
//    func parserHostParam(host: String) -> HostParam? {
//        if host.hasSuffix("localhost:\(self.port)") {
//            let host = host.replacingOccurrences(of: "localhost\(self.port)", with: "")
//            let hostArr = host.split(separator: ".")
//
//            if hostArr.count >= 3 {
//                let dwebPart = hostArr.last
//
//                if !dwebPart!.hasPrefix("dweb") {
//                    return nil
//                }
//                guard let port = Int(dwebPart!.replacingOccurrences(of: "dweb-", with: "")) else {
//                    return nil
//                }
//                let mmid = hostArr[hostArr.count-3...hostArr.count-1].joined(separator: ".") + ".dweb"
//                let subdomain = hostArr[0...hostArr.count-3].joined(separator: ".")
//
//                return HostParam(port: port, mmid: mmid, subdomain: subdomain)
//            } else {
//                return nil
//            }
//        } else {
//            return nil
//        }
//    }
//
//    func getHost(hostOption: HostParam) -> String {
//        return "\(hostOption.subdomain).\(hostOption.mmid)-\(hostOption.port).\(HttpServer.address!)"
//    }
//
//    deinit {
//        HttpServer.app.shutdown()
//    }
//}

