//
//  LeadScriptHandle.swift
//  DWebBrowser
//
//  Created by mac on 2022/3/28.
//

import UIKit
import WebKit

class LeadScriptHandle: NSObject, WKScriptMessageHandler {
    
    var scriptDelegate: WKScriptMessageHandler?
    
    func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
        self.scriptDelegate?.userContentController(userContentController, didReceive: message)
    }
    

    init(messageHandle: WKScriptMessageHandler) {
        super.init()
        self.scriptDelegate = messageHandle
    }
}
