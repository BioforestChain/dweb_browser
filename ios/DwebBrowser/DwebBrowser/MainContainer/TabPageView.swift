//
//  TabPageView.swift
//  TableviewDemo
//
//  Created by ui06 on 4/16/23.
//

import SwiftUI
import WebKit

//层级关系  最前<-- 快照(缩放动画）|| collecitionview  ||  tabPage ( homepage & webview)

struct TabPageView: View {
    @State var showWebview = false
    var body: some View {
        ZStack{
            if showWebview{
                WebPage()
            }
            else{
                HomePage()
                    .background(.pink)
            }
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

struct WebPage: View{
    @State var url: URL = URL(string: "https://www.apple.com")!
    
    var body: some View{
        WebView(url: $url)
    }
}

struct TabPageHStack: View{
    @Binding var adressBarHstackOffset:CGFloat
    @EnvironmentObject var expState: TabPagesExpandState
    //    @EnvironmentObject var addressBarOffset: AddressBarHStackOffset
    
    var body: some View{
        if !expState.state{
            TabsCollectionView().background(.secondary)
            
        }else{
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 0) {
                    TabPageView()
                        .frame(width: screen_width)
                    TabPageView()
                        .frame(width: screen_width)
                    TabPageView()
                        .frame(width: screen_width)
                }
                .offset(x:adressBarHstackOffset)
                
                //                    .offset(x:addressBarOffset.offset)
                .onAppear {
                    //                        UIScrollView.appearance().isPagingEnabled = true
                }
            }
        }
    }
}

struct TabPageView_Previews: PreviewProvider {
    static var previews: some View {
        Text("problem")
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
