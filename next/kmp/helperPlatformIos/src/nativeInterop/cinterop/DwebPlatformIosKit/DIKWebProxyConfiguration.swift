//
//  DIKWebProxyConfiguration.swift
//  DwebPlatformIosKit
//
//  Created by instinct on 2023/11/20.
//

import Foundation
import WebKit

@objc public class DIKWebProxyConfiguration: NSObject {
    
    @objc public func createConfiguration(url: String, port: UInt16) -> WKWebsiteDataStore {
        let endpoint = NWEndpoint.hostPort(host: NWEndpoint.Host(url) , port: NWEndpoint.Port(rawValue: port)!)
        var proxyConfig = ProxyConfiguration.init(httpCONNECTProxy: endpoint, tlsOptions: .none)
        let websiteDataStore = WKWebsiteDataStore.default()
        websiteDataStore.proxyConfigurations = [proxyConfig]
        return websiteDataStore
    }
}

