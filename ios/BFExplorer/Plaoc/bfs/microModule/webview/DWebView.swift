//
//  DWebView.swift
//  BFExplorer
//
//  Created by ui08 on 2023/2/27.
//

import Foundation
import WebKit

class DWebView: WKWebView {
    let mm: MicroModule
    let options: Options
    
    init(mm: MicroModule, options: Options) {
        self.mm = mm
        self.options = options
        
        let url = URL(string: options.loadUrl)
        if url == nil {
            fatalError("DWebview load url error: \(options)")
        }
        
        
        
        
        load(URLRequest(url: url!))
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    struct Options {
        let dwebHost: String
        let loadUrl: String
    }
    
    
}
