//
//  webViewRedrawView.swift
//  DwebBrowser
//
//  Created by ui06 on 4/28/23.
//

import SwiftUI
import WebKit
//struct webViewRedrawView: View {
//    var body: some View {
//        Text(/*@START_MENU_TOKEN@*/"Hello, World!"/*@END_MENU_TOKEN@*/)
//    }
//}

struct webViewRedrawView: View {
    @State private var showWebView = false
    
    var body: some View {
        VStack {
            Button("Show WebView") {
                self.showWebView.toggle()
            }
            
            if showWebView {
                WebView_ex(url: "https://www.google.com")
            }
        }
    }
}

struct WebView_ex: UIViewRepresentable {
    
    let url: String
        
    func makeUIView(context: Context) -> WKWebView {
        return WKWebView()
    }
    
    func updateUIView(_ uiView: WKWebView, context: Context) {
        if let url = URL(string: url) {
            let request = URLRequest(url: url)
            uiView.load(request)
        }
    }
}

struct webViewRedrawView_Previews: PreviewProvider {
    static var previews: some View {
        webViewRedrawView()
    }
}
