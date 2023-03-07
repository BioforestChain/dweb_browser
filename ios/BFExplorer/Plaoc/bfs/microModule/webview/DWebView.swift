//
//  DWebView.swift
//  BFExplorer
//
//  Created by ui08 on 2023/2/27.
//

import Foundation
import WebKit
import Vapor

class DWebView: WKWebView {
    let mm: MicroModule
    let options: Options
    
    init(mm: MicroModule, options: Options, frame: CGRect) {
        self.mm = mm
        self.options = options
        
        let config = DWebView.setUA(mm: mm, options: options)
        super.init(frame: frame, configuration: config)
        self.scrollView.contentInsetAdjustmentBehavior = .never
        self.allowsBackForwardNavigationGestures = true
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    struct Options {
        /**
         * 要加载的页面
         */
        let url: String
    }
    
    private let readyPo = PromiseOut<Bool>()
    func afterReady() async -> Bool {
        await readyPo.waitPromise()
    }
    
    private class func setUA(mm: MicroModule, options: Options) -> WKWebViewConfiguration {
        let baseDwebHost = mm.mmid
        var dwebHost = baseDwebHost
        
        // 初始化设置 ua，这个是无法动态修改的
        let url = URL(string: options.url)
        
        if url == nil || url!.host == nil {
            fatalError("load url is incorrect: \(options.url)")
        }
        
        if url!.scheme == "http" && url!.host!.hasSuffix(".dweb") {
            dwebHost = url!.authority()
        }
        
        if !dwebHost.contains(where: { $0 == ":" }) {
            dwebHost += ":80"
        }

        let config = WKWebViewConfiguration()
        config.applicationNameForUserAgent = "dweb-host/\(dwebHost)"
//        config.setValue(true, forKey: "allowUniversalAccessFromFileURLs")
        
        let preference = WKPreferences()
        preference.javaScriptCanOpenWindowsAutomatically = true
        config.preferences = preference
        
        return config
    }
}
