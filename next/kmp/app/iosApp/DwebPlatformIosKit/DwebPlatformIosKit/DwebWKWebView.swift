//
//  DwebWKWebView.swift
//  DwebPlatformIosKit
//
//  Created by bfs-kingsword09 on 2023/12/12.
//

import WebKit

@objc(DwebWKWebView)
public class DwebWKWebView: WKWebView {
    @objc public dynamic var icon = NSString("")

    override init(frame: CGRect, configuration: WKWebViewConfiguration) {
        super.init(frame: frame, configuration: configuration)
    }

    convenience init() {
        self.init(frame: UIScreen.main.bounds, configuration: WKWebViewConfiguration())
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}
