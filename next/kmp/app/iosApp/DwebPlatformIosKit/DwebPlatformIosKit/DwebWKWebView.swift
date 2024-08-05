//
//  DwebWKWebView.swift
//  DwebPlatformIosKit
//
//  Created by bfs-kingsword09 on 2023/12/12.
//

import WebKit

@objc(DwebWKWebView)
public class DwebWKWebView: WKWebView {
    /**
     * WKWebView 没有 icon ，只有 title，所以这里定义了，由 kotlin 中的 DWebView 来实现它
     */
    @objc public dynamic var icon = NSString("")

    override init(frame: CGRect, configuration: WKWebViewConfiguration) {
        super.init(frame: frame, configuration: configuration)

        /// 默认禁用对于键盘的响应，由内核来主动控制
        NotificationCenter.default.removeObserver(self, name: UIResponder.keyboardWillChangeFrameNotification, object: nil)
        NotificationCenter.default.removeObserver(self, name: UIResponder.keyboardWillShowNotification, object: nil)
        NotificationCenter.default.removeObserver(self, name: UIResponder.keyboardWillHideNotification, object: nil)
    }

    convenience init() {
        self.init(frame: UIScreen.main.bounds, configuration: WKWebViewConfiguration())
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    deinit {
        destroy()
    }

    @objc public func destroy() {}
}
