//
//  TabPageView.swift
//  TableviewDemo
//
//  Created by ui06 on 4/16/23.
//

import SwiftUI

//层级关系  最前<-- 快照(缩放动画）<-- collecitionview  <--  tabPage ( homepage & webview)


struct TabPageView: View {
    
    @ObservedObject var webViewStore: WebViewStore

    @State var homeview = HomeView()

    var  body: some View {
        ZStack{
            NavigationView {
//                Text("there is a webview")
                WebView(webView: webViewStore.webView, url:URL(string: webViewStore.web.openedUrl!)!)
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
                    .background(.orange)
            }.onAppear {
//                
                print("webViewStore.webView ---- \(webViewStore.webView)")
//                self.webViewStore.webView.load(URLRequest(url: URL(string: "163.com")!))
            }
            
            if webViewStore.web.openedUrl == nil{
                homeview
            }
        }
    }
    
    func goBack() {
        webViewStore.webView.goBack()
    }
    
    func goForward() {
        webViewStore.webView.goForward()
    }
}

struct TabPageView_Previews: PreviewProvider {
    static var previews: some View {
        Text("problem")
    }
}
