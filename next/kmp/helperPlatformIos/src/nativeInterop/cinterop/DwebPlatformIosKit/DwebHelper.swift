//
//  DIKProxyConfiguration.swift
//  DwebPlatformIosKit
//
//  Created by bfs-kingsword09 on 2023/11/20.
//

import Foundation
import WebKit

@objc open class DwebHelper: NSObject {
    @objc open func setProxy(configuration: WKWebViewConfiguration, host: String, port: UInt16) {
        let endpoint = NWEndpoint.hostPort(host: NWEndpoint.Host(host), port: NWEndpoint.Port(rawValue: port)!)
        var proxyConfig = ProxyConfiguration(httpCONNECTProxy: endpoint, tlsOptions: .none)
        configuration.websiteDataStore.proxyConfigurations = [proxyConfig]
    }
}

@objc open class URLSchemeTaskHelper: NSObject {
    var taskMap = [URLRequest: EasyURLSchemeTask]()
    @objc open func startURLSchemeTask(_ webView: WKWebView, task: WKURLSchemeTask) -> EasyURLSchemeTask {
        let easy = EasyURLSchemeTask(webView: webView, task: task)
        taskMap[task.request] = easy
        return easy
    }

    @objc open func stopURLSchemeTask(_ webView: WKWebView, task: WKURLSchemeTask) -> Bool {
        if let easy = taskMap.removeValue(forKey: task.request) {
            easy.stopTask()
            return true
        }
        return false
    }
}

@objc open class EasyURLSchemeTask: NSObject {
    let webView: WKWebView
    let task: WKURLSchemeTask
    init(webView: WKWebView, task: WKURLSchemeTask) {
        self.webView = webView
        self.task = task
    }

    private var isStoped = false
    func stopTask() {
        isStoped = true
    }

    @objc open func didReceiveResponse(_ response: URLResponse) -> Bool {
        if isStoped { return false }
        task.didReceive(response)
        return true
    }

    @objc open func didReceiveData(_ data: Data) -> Bool {
        if isStoped { return false }
        task.didReceive(data)
        return true
    }

    @objc open func didFinish() -> Bool {
        if isStoped { return false }
        task.didFinish()
        return true
    }

    @objc open func didFailWithError(_ error: Error) -> Bool {
        if isStoped { return false }
        task.didFailWithError(error)
        return true
    }
}
