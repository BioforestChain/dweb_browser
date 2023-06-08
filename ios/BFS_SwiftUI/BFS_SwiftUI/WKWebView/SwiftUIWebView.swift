//
//  SwiftUIWebView.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/4/24.
//

import SwiftUI
import WebKit

struct SwiftUIWebView: UIViewRepresentable {
    
    typealias UIViewType = WKWebView
    
    let webView: WKWebView
    func makeUIView(context: Context) -> WKWebView {
        
        return webView
    }
    
    func updateUIView(_ uiView: WKWebView, context: Context) {

    }
}


