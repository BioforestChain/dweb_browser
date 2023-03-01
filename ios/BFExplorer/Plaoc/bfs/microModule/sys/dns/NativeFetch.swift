//
//  NativeFetch.swift
//  BFExplorer
//
//  Created by ui08 on 2023/2/21.
//

import Foundation
import Vapor
import Alamofire

typealias FetchAdaptor = (_ remote: MicroModule, _ request: Vapor.Request) -> Vapor.Response?
struct S_FetchAdaptor: Hashable {
    var timestamp = Date().milliStamp
    var fetchAdaptor: FetchAdaptor
    
    func hash(into hasher: inout Hasher) {
        hasher.combine(timestamp)
    }
    
    static func ==(lhs: S_FetchAdaptor, rhs: S_FetchAdaptor) -> Bool {
        return lhs.timestamp == rhs.timestamp
    }
}

class NativeFetchAdaptersManager {
    private var adapterOrderMap: [S_FetchAdaptor:Int] = [:]
    private var orderdAdapters: [S_FetchAdaptor] = []
    
    var adapters: [S_FetchAdaptor] {
        get {
            orderdAdapters
        }
    }
    
    func append(order: Int = 0, adapter: @escaping FetchAdaptor) -> () -> Bool {
        let fetchAdaptor = S_FetchAdaptor(fetchAdaptor: adapter)
        adapterOrderMap[fetchAdaptor] = order
        orderdAdapters = adapterOrderMap.reduce(into: []) { $0.append(($1.key, $1.value)) }.sorted(by: { $0.1 < $1.1 }).map { $0.0 }
        
        return {
            return self.remove(fetchAdaptor: fetchAdaptor)
        }
    }
    
    func remove(fetchAdaptor: S_FetchAdaptor) -> Bool {
        adapterOrderMap.removeValue(forKey: fetchAdaptor)
        return true
    }
}

var nativeFetchAdaptersManager = NativeFetchAdaptersManager()

func localeFileFetch(remote: MicroModule, request: Vapor.Request) -> Vapor.Response? {
    if request.url.scheme == "file" && request.url.host == "", let url = URL(string: request.url.string) {
        let path = url.pathComponents.joined(separator: "/")
        
        return request.fileio.streamFile(at: Bundle.main.bundlePath + "/app/sdk\(path)")
    } else {
        return nil
    }
}

func alamofireFetch(request: Vapor.Request) -> Vapor.Response? {
    do {
        let semaphore = DispatchSemaphore(value: 0)
        var headers: [Alamofire.HTTPHeader] = []
        request.headers.forEach { headers.append(Alamofire.HTTPHeader(name: $0.0, value: $0.1)) }
        var req = try URLRequest(url: request.url.string, method: Alamofire.HTTPMethod(rawValue: request.method.rawValue))
        req.headers = Alamofire.HTTPHeaders(headers)
        
        if request.body.data != nil {
            var data = request.body.data!
            req.httpBody = data.readData(length: data.readableBytes)
        } else if request.method == .POST || request.method == .PUT || request.method == .PATCH {
            var req_body_stream: Data = Data()
            var sequential = request.eventLoop.makeSucceededFuture(())
            
            request.body.drain {
                switch $0 {
                case .buffer(var buffer):
                    if buffer.readableBytes > 0 {
                        req_body_stream.append(buffer.readData(length: buffer.readableBytes)!)
                    }
                    
                    return sequential
                case .error(_):
                    return sequential
                case .end:
                    return sequential
                }
            }
            
            
            req.httpBodyStream = InputStream(data: req_body_stream)
        }
        
        var response: Vapor.Response = Response(status:.badRequest)
        let task = URLSession.shared.dataTask(with: req) { (data, res, error) in
            if error != nil {
                response = Response(status: .badRequest)
            } else {
                response = Response(status: .ok, headers: request.headers, body: .init(data: data!))
            }
            
            semaphore.signal()
        }
        
        semaphore.wait()
        
        return response
    } catch {
        return nil
    }
}

extension MicroModule {
    func nativeFetch(request: Vapor.Request) -> Vapor.Response {
        for fetchAdapter in nativeFetchAdaptersManager.adapters {
            let response = fetchAdapter.fetchAdaptor(self, request)
            if response != nil {
                return response!
            }
        }
        
        return localeFileFetch(remote: self, request:request) ?? alamofireFetch(request: request)!
    }
    
    func nativeFetch(url: URI) -> Vapor.Response {
        
        nativeFetch(request: Vapor.Request(application: HttpServer.app, method: .GET, url: url, on: HttpServer.app.eventLoopGroup.next()))
    }
    
    func nativeFetch(url: String) -> Vapor.Response {
        nativeFetch(request: Vapor.Request(application: HttpServer.app, method: .GET, url: URI(string: url), on: HttpServer.app.eventLoopGroup.next()))
    }
}

