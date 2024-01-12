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
        let endpoint = NWEndpoint.hostPort(
            host: NWEndpoint.Host(host), port: NWEndpoint.Port(rawValue: port)!)
        var proxyConfig = ProxyConfiguration(httpCONNECTProxy: endpoint, tlsOptions: .none)
        configuration.websiteDataStore.proxyConfigurations = [proxyConfig]
    }

    @objc open func enableSafeAreaInsets(webView: WKWebView, insets: UIEdgeInsets) {
        webView.setValue(true, forKey: "_haveSetUnobscuredSafeAreaInsets")
        webView.setValue(insets, forKey: "_unobscuredSafeAreaInsets")
    }

    @objc open func disableSafeAreaInsets(webView: WKWebView) {
        webView.setValue(false, forKey: "_haveSetUnobscuredSafeAreaInsets")
    }
}

@objc open class URLSchemeTaskHelper: NSObject {
    var taskMap = [URLRequest: EasyURLSchemeTask]()
    @objc open func startURLSchemeTask(_ webView: WKWebView, task: WKURLSchemeTask)
        -> EasyURLSchemeTask
    {
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
        do {
            try ObjC.catchException {
                self.task.didReceive(response)
            }
        } catch {
            return false
        }
        return true
    }

    @objc open func didReceiveData(_ data: Data) -> Bool {
        if isStoped { return false }
        do {
            try ObjC.catchException {
                self.task.didReceive(data)
            }
        } catch {
            return false
        }
        return true
    }

    @objc open func didFinish() -> Bool {
        if isStoped { return false }
        do {
            try ObjC.catchException {
                self.task.didFinish()
            }
        } catch {
            return false
        }
        return true
    }

    @objc open func didFailWithError(_ error: Error) -> Bool {
        if isStoped { return false }
        do {
            try ObjC.catchException {
                self.task.didFailWithError(error)
            }
        } catch {
            return false
        }
        return true
    }
}

@objc open class BgPlaceholderView: UIView {
    private var willMoveNewSuperview: ((UIView?) -> Void)? = nil
    @objc open func setCallback(_ willMoveNewSuperview: @escaping (UIView?) -> Void) {
        self.willMoveNewSuperview = willMoveNewSuperview
    }

    override open func willMove(toSuperview newSuperview: UIView?) {
        super.willMove(toSuperview: newSuperview)
        willMoveNewSuperview?(newSuperview)
    }
}

/**
 * 原理参考 https://github.com/RyukieSama/Swifty/blob/master/Swifty/Classes/UIKit/UIView/View/ScreenShieldView.swift
 */
@objc open class SecureViewController: NSObject {
    private let vc: UIViewController
    private let field = UITextField()

    @objc public init(vc: UIViewController, onNewView: ((UIView) -> Void)?) {
        self.vc = vc
        let oldView = vc.view!
        let newView = field.subviews.first!
        while let subview = field.subviews.first {
            subview.removeFromSuperview()
        }
        DispatchQueue.main.async {
            newView.frame = oldView.frame
            newView.bounds = oldView.bounds
            newView.isUserInteractionEnabled = oldView.isUserInteractionEnabled
            onNewView?(newView)

            // 替换 viewController 的 rootView
            vc.view = newView
            // 将原本的view附加到新的view内
            newView.addSubview(oldView)

            /// 加入布局约束
            let layoutDefaultLowPriority = UILayoutPriority(rawValue: UILayoutPriority.defaultLow.rawValue-1)
            let layoutDefaultHighPriority = UILayoutPriority(rawValue: UILayoutPriority.defaultHigh.rawValue-1)

            oldView.translatesAutoresizingMaskIntoConstraints = false
            oldView.setContentHuggingPriority(layoutDefaultLowPriority, for: .vertical)
            oldView.setContentHuggingPriority(layoutDefaultLowPriority, for: .horizontal)
            oldView.setContentCompressionResistancePriority(layoutDefaultHighPriority, for: .vertical)
            oldView.setContentCompressionResistancePriority(layoutDefaultHighPriority, for: .horizontal)

            let top = NSLayoutConstraint(item: oldView, attribute: .top, relatedBy: .equal, toItem: newView, attribute: .top, multiplier: 1, constant: 0)
            let bottom = NSLayoutConstraint(item: oldView, attribute: .bottom, relatedBy: .equal, toItem: newView, attribute: .bottom, multiplier: 1, constant: 0)
            let leading = NSLayoutConstraint(item: oldView, attribute: .leading, relatedBy: .equal, toItem: newView, attribute: .leading, multiplier: 1, constant: 0)
            let trailing = NSLayoutConstraint(item: oldView, attribute: .trailing, relatedBy: .equal, toItem: newView, attribute: .trailing, multiplier: 1, constant: 0)

            newView.addConstraints([top, bottom, leading, trailing])
        }
    }

    @objc open func setSafeMode(_ safe: Bool) {
        field.isSecureTextEntry = safe
    }
}
