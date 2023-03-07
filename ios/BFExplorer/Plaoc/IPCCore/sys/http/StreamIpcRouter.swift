//
//  StreamIpcRouter.swift
//  BFExplorer
//
//  Created by ui03 on 2023/3/3.
//

import UIKit
import Vapor

class StreamIpcRouter: NSObject {

    private var config: RouteConfig
    private var streamIpc: ReadableStreamIpc
    
    init(config: RouteConfig, streamIpc: ReadableStreamIpc) {
        self.config = config
        self.streamIpc = streamIpc
    }
    
    lazy var isMatch: (URLRequest) -> Bool = {
        if config.matchMode == .PREFIX {
            let match = { (request: URLRequest) -> Bool in
                guard request.url != nil else { return false }
                return request.httpMethod == self.config.method && request.url!.path.hasPrefix(self.config.pathname)
            }
            return match
        } else {
            let match = { (request: URLRequest) -> Bool in
                guard request.url != nil else { return false }
                return request.httpMethod == self.config.method && request.url!.path == self.config.pathname
            }
            return match
        }
    }()
    
    func handler(request: URLRequest) -> Response? {
        if isMatch(request) {
            return streamIpc.request(request: request)
        }
        return nil
    }
}
