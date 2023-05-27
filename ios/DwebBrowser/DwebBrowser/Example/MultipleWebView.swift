//
//  MultipleWeBview.swift
//  DwebBrowser
//
//  Created by ui06 on 5/26/23.
//

import SwiftUI
import WebKit
// 定义一个单例模式，用于管理所有的WKWebView对象
//struct IDWebView: Hashable{
//    let uuid: UUID
//    let webView: WKWebView
//}
//
//class WebViewManager {
//    static let shared = WebViewManager()
//    var webViews = Set<IDWebView>()
//    func webView(of expectedId: UUID) -> WKWebView{
//        if let idWebView = webViews.filter({ $0.uuid == expectedId}).first{
//            return idWebView.webView
//        }else{
//            let idWebView = IDWebView(uuid: expectedId, webView: WKWebView())
//            webViews.insert(idWebView)
//            return idWebView.webView
//        }
//    }
//    private init() {}
//}

// 定义一个包装WKWebView的视图
struct WebView2: UIViewRepresentable {
    var webcache2: WebCache
//    let request: URLRequest
    init(webcache: WebCache){
        print("making WebView2....")
        webcache2 = webcache
    }

    func makeUIView(context: Context) -> WKWebView {
        // 从WebViewManager中获取已有的WKWebView对象
        let webView = WebWrapperManager.shared.webWrapper(of: webcache2.id).webView
        webView.load(URLRequest(url:webcache2.lastVisitedUrl))
//        WebViewManager.shared.webViews.insert(webView)
        return webView
    }

    func updateUIView(_ uiView: WKWebView, context: Context) {
        uiView.load(URLRequest(url:webcache2.lastVisitedUrl))
    }
    
}

struct MultipleWebView: View {
    @State var showWebView = true
    let colors:[Color] = [.red,.orange,.yellow,.green,.purple]
    var body: some View {
        VStack{
            ZStack{
                    ScrollView(.horizontal) {
                        HStack(spacing: 0) {
                            ForEach(WebCacheStore.shared.store, id: \.id) { webCache in
                                WebView2(webcache: webCache)
                                    .frame(width: UIScreen.main.bounds.width, height: UIScreen.main.bounds.height - 150)
                                    .background(colors[WebCacheStore.shared.store.firstIndex(of: webCache) ?? 0])
                                    .padding()
                            }.background(.blue)
                        }.background(.red)
                    }.background(.gray)
                
                .opacity(showWebView ? 1 : 0)
                    Color.cyan
                    .hidden()
            }
            
            Button("hide") {
                showWebView.toggle()
            }
        }
    }
}

struct MultipleWebView_Previews: PreviewProvider {
    static var previews: some View {
        MultipleWebView()
    }
}
