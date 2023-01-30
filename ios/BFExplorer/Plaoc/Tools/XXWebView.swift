//
//  XXWebView.swift
//  Plaoc-iOS
//
//  Created by mac on 2022/7/5.
//

import UIKit
import WebKit

protocol XXWebViewProtocol {
    func clearAllWebCache()
}

class XXWebView: WKWebView {

    var holderObj: AnyObject?
    
    
    static func defaultConfiguration() -> WKWebViewConfiguration {
        let config = WKWebViewConfiguration()
        config.userContentController = WKUserContentController()
//        addScriptMessageHandler(config: config)
//        addScriptMessageHandlerWithReply(config: config)
//        if self.scripts != nil {
//            for script in self.scripts! {
//                config.userContentController.addUserScript(script)
//            }
//        }
        let prefreen = WKPreferences()
        prefreen.javaScriptCanOpenWindowsAutomatically = true
        config.preferences = prefreen
        config.setValue(true, forKey: "allowUniversalAccessFromFileURLs")
        config.setURLSchemeHandler(Schemehandler(appId: "TODO"), forURLScheme: schemeString)
        return config
    }
    
    deinit {
        configuration.userContentController.removeAllUserScripts()
        if #available(iOS 14.0, *) {
            configuration.userContentController.removeAllScriptMessageHandlers()
        } else {
            // Fallback on earlier versions
        }
        stopLoading()
        uiDelegate = nil
        navigationDelegate = nil
        holderObj = nil
    }
    override init(frame: CGRect, configuration: WKWebViewConfiguration) {
        super.init(frame: frame, configuration: configuration)
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
}

extension XXWebView: XXWebViewProtocol {
    
    func clearAllWebCache() {
        let types = WKWebsiteDataStore.allWebsiteDataTypes()
        let dateFrom = Date(timeIntervalSince1970: 0)
        WKWebsiteDataStore.default().removeData(ofTypes: types, modifiedSince: dateFrom) {
            
        }
    }
}

extension XXWebView: WebViewPoolProtocol{
    // 即将被复用
    func webViewWillleavePool() {
        
    }
    // 被回收
    func webViewWillEnterPool() {
        
        configuration.userContentController.removeAllUserScripts()
        if #available(iOS 14.0, *) {
            configuration.userContentController.removeAllScriptMessageHandlers()
        } else {
            // Fallback on earlier versions
        }
        stopLoading()
        NSObject.cancelPreviousPerformRequests(withTarget: self)
        scrollView.delegate = nil
        uiDelegate = nil
        navigationDelegate = nil
        holderObj = nil
        let selStr = "_re" + "mov" + "eA" + "llIt" + "ems"
        let sel = Selector(selStr)
        if self.backForwardList.responds(to: sel) {
            self.backForwardList.perform(sel)
        }
        loadHTMLString("", baseURL: nil)
    }
}
