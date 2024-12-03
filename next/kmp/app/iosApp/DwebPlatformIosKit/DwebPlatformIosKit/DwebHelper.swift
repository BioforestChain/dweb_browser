//
//  DIKProxyConfiguration.swift
//  DwebPlatformIosKit
//
//  Created by bfs-kingsword09 on 2023/11/20.
//

import Foundation
import WebKit

@objcMembers open class DwebHelper: NSObject {
    open func setProxy(websiteDataStore: WKWebsiteDataStore, host: String, port: UInt16) {
        let endpoint = NWEndpoint.hostPort(
            host: NWEndpoint.Host(host), port: NWEndpoint.Port(rawValue: port)!)
        let proxyConfig = ProxyConfiguration(httpCONNECTProxy: endpoint, tlsOptions: .none)
        websiteDataStore.proxyConfigurations = [proxyConfig]
    }

    open func enableSafeAreaInsets(webView: WKWebView, insets: UIEdgeInsets) {
        webView.setValue(true, forKey: "_haveSetUnobscuredSafeAreaInsets")
        webView.setValue(insets, forKey: "_unobscuredSafeAreaInsets")
    }

    open func disableSafeAreaInsets(webView: WKWebView) {
        webView.setValue(false, forKey: "_haveSetUnobscuredSafeAreaInsets")
    }
    
    open func openURL(_ url: URL) async -> Bool {
        return await UIApplication.shared.open(url)
    }
}

@objcMembers open class URLSchemeTaskHelper: NSObject {
    var taskMap = [URLRequest: EasyURLSchemeTask]()
    open func startURLSchemeTask(_ webView: WKWebView, task: WKURLSchemeTask)
        -> EasyURLSchemeTask
    {
        let easy = EasyURLSchemeTask(webView: webView, task: task)
        taskMap[task.request] = easy
        return easy
    }

    open func stopURLSchemeTask(_ webView: WKWebView, task: WKURLSchemeTask) -> Bool {
        if let easy = taskMap.removeValue(forKey: task.request) {
            easy.stopTask()
            return true
        }
        return false
    }
}

@objcMembers open class EasyURLSchemeTask: NSObject {
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

    open func didReceiveResponse(_ response: URLResponse) -> Bool {
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

    open func didReceiveData(_ data: Data) -> Bool {
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

    open func didFinish() -> Bool {
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

    open func didFailWithError(_ error: Error) -> Bool {
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

class ProxyUIView: UIView {
    var target: UIView? = nil

    override func addSubview(_ view: UIView) {
        if let proxyView = target {
            print("QAQ proxyUIView addSubview \(view) to \(proxyView)")
            proxyView.addSubview(view)
        } else {
            super.addSubview(view)
        }
    }
    override func insertSubview(_ view: UIView, at index: Int) {
        if let proxyView = target {
            proxyView.insertSubview(view, at: index)
        } else {
            super.insertSubview(view, at: index)
        }
    }
    override func insertSubview(_ view: UIView, aboveSubview siblingSubview: UIView) {
        if let proxyView = target {
            proxyView.insertSubview(view, aboveSubview: siblingSubview)
        } else {
            super.insertSubview(view, aboveSubview: siblingSubview)
        }
    }
    override func insertSubview(_ view: UIView, belowSubview siblingSubview: UIView) {
        if let proxyView = target {
            proxyView.insertSubview(view, belowSubview: siblingSubview)
        } else {
            super.insertSubview(view, belowSubview: siblingSubview)
        }
    }
    override func hitTest(_ point: CGPoint, with event: UIEvent?) -> UIView? {
        if let proxyView = target {
            return proxyView.hitTest(point, with: event)
        }
        return super.hitTest(point, with: event)
    }
}

/**
 * 原理参考 https://github.com/RyukieSama/Swifty/blob/master/Swifty/Classes/UIKit/UIView/View/ScreenShieldView.swift
 */
@objcMembers open class SecureViewController: NSObject {
    private let vc: UIViewController
    private let field = UITextField()
    var dosets = [() -> Void]()
    var unsets = [() -> Void]()

    public init(vc: UIViewController, onNewView: ((UIView) -> Void)?) {
        self.vc = vc
        let oldView = vc.view!
        let newView = ProxyUIView()
        let safeView = field.subviews.first!
        /// 清空field的子视图
        while let subview = field.subviews.first {
            subview.removeFromSuperview()
        }

        DispatchQueue.main.async {
            /// 设置代理视图的大小与交互
            newView.frame = oldView.frame
            newView.bounds = oldView.bounds
            newView.isUserInteractionEnabled = oldView.isUserInteractionEnabled
            safeView.frame = oldView.frame
            safeView.bounds = oldView.bounds
            safeView.isUserInteractionEnabled = oldView.isUserInteractionEnabled

            /// 将安全视图添加到代理视图中，并设置布局关系
            newView.addSubview(safeView)

            let top = NSLayoutConstraint(item: safeView, attribute: .top, relatedBy: .equal, toItem: newView, attribute: .top, multiplier: 1, constant: 0)
            let bottom = NSLayoutConstraint(item: safeView, attribute: .bottom, relatedBy: .equal, toItem: newView, attribute: .bottom, multiplier: 1, constant: 0)
            let leading = NSLayoutConstraint(item: safeView, attribute: .leading, relatedBy: .equal, toItem: newView, attribute: .leading, multiplier: 1, constant: 0)
            let trailing = NSLayoutConstraint(item: safeView, attribute: .trailing, relatedBy: .equal, toItem: newView, attribute: .trailing, multiplier: 1, constant: 0)

            newView.addConstraints([top, bottom, leading, trailing])

            /// 设置代理对象
            newView.target = oldView
        }

        unsets.append {
            vc.view = oldView
        }
        dosets.append {
            // 将 vc 的 view 设置成 代理视图，这个视图会代理 内容视图的 addSubView
            vc.view = newView
            // 将内容视图添加到 safeView 中，使得 isSecureTextEntry 属性会保护视图不被截取
            safeView.addSubview(oldView)
        }

        /// 加入布局约束
        let oldTranslatesAutoresizingMaskIntoConstraints = oldView.translatesAutoresizingMaskIntoConstraints
        let oldContentHuggingPriorityVertical = oldView.contentHuggingPriority(for: .vertical)
        let oldContentHuggingPriorityHorizontal = oldView.contentHuggingPriority(for: .horizontal)
        let oldContentCompressionResistancePriorityVertical = oldView.contentCompressionResistancePriority(for: .vertical)
        let oldContentCompressionResistancePriorityHorizontal = oldView.contentCompressionResistancePriority(for: .horizontal)

        unsets.append {
            oldView.translatesAutoresizingMaskIntoConstraints = oldTranslatesAutoresizingMaskIntoConstraints
            oldView.setContentHuggingPriority(oldContentHuggingPriorityVertical, for: .vertical)
            oldView.setContentHuggingPriority(oldContentHuggingPriorityHorizontal, for: .horizontal)
            oldView.setContentCompressionResistancePriority(oldContentCompressionResistancePriorityVertical, for: .vertical)
            oldView.setContentCompressionResistancePriority(oldContentCompressionResistancePriorityHorizontal, for: .horizontal)
        }
        ///
        let layoutDefaultLowPriority = UILayoutPriority(rawValue: UILayoutPriority.defaultLow.rawValue-1)
        let layoutDefaultHighPriority = UILayoutPriority(rawValue: UILayoutPriority.defaultHigh.rawValue-1)

        dosets.append {
            oldView.translatesAutoresizingMaskIntoConstraints = false
            oldView.setContentHuggingPriority(layoutDefaultLowPriority, for: .vertical)
            oldView.setContentHuggingPriority(layoutDefaultLowPriority, for: .horizontal)
            oldView.setContentCompressionResistancePriority(layoutDefaultHighPriority, for: .vertical)
            oldView.setContentCompressionResistancePriority(layoutDefaultHighPriority, for: .horizontal)
        }

        let top = NSLayoutConstraint(item: oldView, attribute: .top, relatedBy: .equal, toItem: newView, attribute: .top, multiplier: 1, constant: 0)
        let bottom = NSLayoutConstraint(item: oldView, attribute: .bottom, relatedBy: .equal, toItem: newView, attribute: .bottom, multiplier: 1, constant: 0)
        let leading = NSLayoutConstraint(item: oldView, attribute: .leading, relatedBy: .equal, toItem: newView, attribute: .leading, multiplier: 1, constant: 0)
        let trailing = NSLayoutConstraint(item: oldView, attribute: .trailing, relatedBy: .equal, toItem: newView, attribute: .trailing, multiplier: 1, constant: 0)

        unsets.append {
            newView.removeConstraints([top, bottom, leading, trailing])
        }
        dosets.append {
            newView.addConstraints([top, bottom, leading, trailing])
        }
    }

    open func effect() {
        DispatchQueue.main.async {
            for fun in self.dosets {
                fun()
            }
        }
    }

    open func dispose() {
        DispatchQueue.main.async {
//            self.field.isSecureTextEntry = false
            for fun in self.unsets {
                fun()
            }
        }
    }

    open func setSafeMode(_ safe: Bool) {
        
        field.isSecureTextEntry = safe
        if safe {
            effect()
        } else {
            dispose()
        }
    }
}
