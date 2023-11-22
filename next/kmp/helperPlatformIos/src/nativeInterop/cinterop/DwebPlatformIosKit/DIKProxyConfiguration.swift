//
//  DIKProxyConfiguration.swift
//  DwebPlatformIosKit
//
//  Created by bfs-kingsword09 on 2023/11/20.
//

import Foundation
import WebKit

@objc open class DwebHelper : NSObject {
    @objc open func setProxy(configuration: WKWebViewConfiguration, host: String, port: UInt16) {
        let endpoint = NWEndpoint.hostPort(host: NWEndpoint.Host(host) , port: NWEndpoint.Port(rawValue: port)!)
        var proxyConfig = ProxyConfiguration.init(httpCONNECTProxy: endpoint, tlsOptions: .none)
        configuration.websiteDataStore.proxyConfigurations = [proxyConfig]
    }
}
