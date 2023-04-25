//
//  TabPageView.swift
//  TableviewDemo
//
//  Created by ui06 on 4/16/23.
//

import SwiftUI
import WebKit

struct TabPageView: View {
    @State var isWebVisible = true
    var body: some View {
        ZStack{
            if isWebVisible{
                WebPage(url: URL(string: "https://www.apple.com")!)
            }else{
                HomePage()
            }
        }
        .background(.yellow)
    }
}

struct TabPageView_Previews: PreviewProvider {
    static var previews: some View {
        TabPageHStack(adressBarHstackOffset: .constant(0))
    }
}

struct HomePage: View{
    var body: some View{
        GeometryReader { geometry in // 获取父视图的大小和位置
            Color.clear
            VStack{
                Text("HomepageHeader")
                Spacer()
                Text("Homepage")
                Spacer()
                Text("HomepageFooter")
            }
        }
    }
}

struct WebPage: View{
    @State var url: URL
    
    var body: some View{
        WebView(url: $url)
    }
}

struct TabPageHStack: View{
    @Binding var adressBarHstackOffset:CGFloat
    var body: some View{
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 0) {
                TabPageView()
                    .frame(width: screen_width)
//                    .disabled(false)
                TabPageView()
                    .frame(width: screen_width)
                TabPageView()
                    .frame(width: screen_width)
            }
            
            .offset(x:adressBarHstackOffset)
            .onAppear {
                UIScrollView.appearance().isPagingEnabled = true
            }
        }
    }
}

struct WebView: UIViewRepresentable {
    @Binding var url: URL
    
    func makeUIView(context: Context) -> WKWebView {
        let webView = WKWebView()
        webView.load(URLRequest(url: url))
        return webView
    }
    
    func updateUIView(_ uiView: WKWebView, context: Context) {
        uiView.load(URLRequest(url: url))
    }
}
