//
//  HttpNMM.swift
//  BFExplorer
//
//  Created by ui03 on 2023/3/2.
//

import Foundation
import Vapor
import CoreHaptics

class HttpNMM: NativeMicroModule {
    
    private let dwebServer = HTTPServer()
    private var tokenMap: [String:Gateway] = [:]
    private var gatewayMap: [String:Gateway] = [:]
    
    init() {
        super.init(mmid: "http.sys.dweb")
    }
    
    private func httpHandler(_ request: URLRequest) -> Response? {
        var header_host: String?
        var header_x_dweb_host: String?
        var header_user_agent_host: String?
        let query_x_web_host = request.url?.urlParameters?["X-DWeb-Host"]
        
        for (key,value) in request.allHTTPHeaderFields ?? [:] {
            switch key {
            case "Host":
                header_host = value
            case "X-Dweb-Host":
                header_x_dweb_host = value
            case "User-Agent":
                let result = value.regex(pattern: #"\sdweb-host\/(.+)\s*"#)
                if result != nil {
                    //TODO
                    header_user_agent_host = result
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
    
    override func _bootstrap() throws {
        dwebServer.createServer(22605)
        
        _ = afterShutdownSignal.listen { _ in
            
            let adapter = { (fromMM: MicroModule, request: URLRequest) -> Response? in
                if request.url?.scheme == "http", ((request.url?.host?.hasSuffix(".dweb")) != nil) {
                    //TODO
                    var req = request
                    var headers = req.allHTTPHeaderFields
                    headers?["X-Dweb-Host"] = self.authority(request: req)
                    req.allHTTPHeaderFields = headers
                    let replace = self.replaceHostAndPort(request: request)
                    let result = request.url?.absoluteString.replacingOccurrences(of: replace, with: self.dwebServer.authority)
                    req.url = URL(string: result ?? "")
                    
                    let response = NetworkManager.downLoadBodyByRequest(request: req)
                    return response
                }
                return nil
            }
            let generics = nativeFetchAdaptersManager.append(adapter: GenericsClosure(closure: adapter))
            return generics
        }
        
        let group = dwebServer.app.grouped("\(mmid)")
        
        group.on(.GET, "start") { request -> Response in
            let port = request.query[Int.self, at: "port"] ?? 80
            let subdomain = request.query[String.self] ?? ""
            return self.defineHandler(req: request) { reque, ipc in
                self.start(ipc: ipc, options: DwebHttpServerOptions(port: port, subdomain: subdomain))
            }
        }
        
        group.on(.POST, "listen") { request -> Response in
            let token = request.query[String.self, at: "token"] ?? ""
            let routes = request.query[String.self, at: "routes"] ?? ""
            guard let type_routes = ChangeTools.jsonArrayToModel(jsonStr: routes, RouteConfig.self) as? [RouteConfig] else {
                return self.badResponse(request: request)
            }
            return self.listen(token: token, message: request, routes: type_routes) ?? self.badResponse(request: request)
        }
        
        group.on(.GET, "close") { request -> Response in
            return self.defineHandler(req: request) { reque, ipc in
                let port = request.query[Int.self, at: "port"] ?? 80
                let subdomain = request.query[String.self] ?? ""
                return self.defineHandler(req: request) { reque, ipc in
                    self.close(ipc: ipc, options: DwebHttpServerOptions(port: port, subdomain: subdomain))
                }
            }
        }
    }
    
    private func authority(request: URLRequest) -> String {
        guard let url = request.url else { return "" }
        let port = url.port
        let host = url.host
        let user = url.user
        let password = url.password
        var result = ""
        if user != nil {
            result = user!
        }
        if password != nil {
            result = result.count > 0 ? "\(user!):\(password!)" : password!
        }
        result += "@"
        if host != nil {
            result += host!
        }
        if port != nil {
            result += ":\(port!)"
        }
        return result
    }
    
    private func replaceHostAndPort(request: URLRequest) -> String {
        guard let url = request.url else { return "" }
        let port = url.port
        let host = url.host
        var result = ""
        if host != nil {
            result += host!
        }
        if port != nil {
            result += ":\(port!)"
        }
        return result
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
        let public_origin = dwebServer.origin
        return ServerUrlInfo(host: host, internal_origin: internal_origin, public_origin: public_origin)
    }
    
    override func _shutdown() throws {
        dwebServer.shutdown()
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
        
        let streamIpc = ReadableStreamIpc(remote: remote, role: .SERVER)
        guard let buffer = message.body.data else { return nil }
        guard let data = buffer.getData(at: 0, length: buffer.readableBytes) else { return nil }
        let stream = InputStream(data: data)
        
        streamIpc.bindIncomeStream(stream: stream, coroutineName: "http-gateway")
        for config in routes {
            _ = streamIpc.onClose { _ in
                gateway.listener.addRouter(config: config, streamIpc: streamIpc)
            }
        }
        
        return Response(status: .ok, body: Response.Body(data: data))
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


