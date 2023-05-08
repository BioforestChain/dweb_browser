//
//  WebView.swift
//  DwebBrowser
//
//  Created by ui06 on 5/4/23.
//

import SwiftUI

import WebKit
//import WebView

struct MyWeb: View {
    @StateObject var webViewStore = WebViewStore()
    @State var showwebview: Bool = false
    
    var body: some View {
        //        if showwebview{
        VStack{
            ZStack{
                NavigationView {
                    WebView(webView: webViewStore.webView)
                        .navigationBarTitle(Text(verbatim: webViewStore.title ?? ""), displayMode: .inline)
                        .navigationBarItems(trailing: HStack {
                            Button(action: goBack) {
                                Image(systemName: "chevron.left")
                                    .imageScale(.large)
                                    .aspectRatio(contentMode: .fit)
                                    .frame(width: 32, height: 32)
                            }.disabled(!webViewStore.canGoBack)
                            Button(action: goForward) {
                                Image(systemName: "chevron.right")
                                    .imageScale(.large)
                                    .aspectRatio(contentMode: .fit)
                                    .frame(width: 32, height: 32)
                            }.disabled(!webViewStore.canGoForward)
                        })
                }.onAppear {
                    self.webViewStore.webView.load(URLRequest(url: URL(string: "https://apple.com")!))
                }
                
                Image("")
                    .frame(width:screen_width, height: screen_height)
                    .background(.green)
                    .opacity(showwebview ? 0 : 1)
                
            }
            .frame(width:screen_width, height: screen_height-150)
            
            Button {
                showwebview.toggle()
            } label: {
                Text("tap to hide webview")
            }.frame(width:screen_width, height: 50)
                .background(.red)
//                .opacity(showwebview ? 0 : 1)
        }
    }
    
    
    func goBack() {
        webViewStore.webView.goBack()
    }
    
    func goForward() {
        webViewStore.webView.goForward()
    }
}

struct MyWebView_Previews: PreviewProvider {
    static var previews: some View {
        MyWeb()
    }
}
