//
//  TabPageView.swift
//  TableviewDemo
//
//  Created by ui06 on 4/16/23.
//

import SwiftUI
import WebKit

//层级关系  最前<-- 快照(缩放动画）<-- collecitionview  <--  tabPage ( homepage & webview)

struct TabPageView: View {
    @State var showWebview = false
    @State var webview = WebPage()
    @State var homeview = HomePage()
    var  body: some View {
        ZStack{
            webview
            homeview.opacity(showWebview ? 0 : 1)
        }
        .onAppear {
            webview.load() // 加载网页
        }
        .onDisappear {
            webview.unload() // 销毁网页
        }
    }
}

struct HomePage: View{
    var body: some View{
        Color.clear
        ScrollView(.vertical){
            VStack{
                Text("HomepageHeader")
                Spacer()
                Text("Homepage")
                Spacer()
                Text("HomepageFooter")
            }
        }.background(.yellow)
    }
}

struct WebPage: UIViewRepresentable {
    var url: URL = URL(string: "https://www.apple.com")!
    private var webView = WKWebView() // 定义一个可重用的 WKWebView

    func makeUIView(context: Context) -> WKWebView {

        return webView
    }
    
    func updateUIView(_ uiView: WKWebView, context: Context) {
//        uiView.load(URLRequest(url: url))
    }
    
    func load() {
        let request = URLRequest(url: url)
        webView.load(request)
    }
    
    func unload() {
        webView.stopLoading()
        webView.removeFromSuperview()
    }
}



struct TabPageView_Previews: PreviewProvider {
    static var previews: some View {
        Text("problem")
    }
}
