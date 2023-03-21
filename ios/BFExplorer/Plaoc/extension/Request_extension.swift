//
//  Request_extension.swift
//  BFExplorer
//
//  Created by ui03 on 2023/3/15.
//

import Foundation
import Vapor

extension Request {
    static func new(method: HTTPMethod = .GET, url: String, collectedBody: ByteBuffer? = nil) -> Request {
        var request: Request
        if collectedBody != nil {
            request = Request(application: HttpServer.app,
                    method: method,
                    url: URI(string: url),
                    collectedBody: collectedBody!,
                    on: HttpServer.app.eventLoopGroup.next()
            )
        } else {
            request = Request(application: HttpServer.app,
                                  method: method,
                                  url: URI(string: url),
                                  on: HttpServer.app.eventLoopGroup.next()
            )
        }
        
        request.route = HttpServer.app.routes.all.first(where: { route in
            if request.url.scheme == "file" && request.url.host != nil {
                if route.path.string == (request.url.host! + request.url.path) && route.method == request.method {
                    return true
                }
                
                return false
            } else if request.url.scheme == "http" || request.url.scheme == "https" {
                // TODO: 处理http
                if request.url.host!.hasPrefix("localhost") {
                    
                }
                
                return false
            } else {
                return false
            }
        })
        
        return request
    }
}
