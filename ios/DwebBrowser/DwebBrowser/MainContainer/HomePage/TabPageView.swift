//
//  TabPageView.swift
//  TableviewDemo
//
//  Created by ui06 on 4/16/23.
//

import SwiftUI
import WebView

//层级关系  最前<-- 快照(缩放动画）<-- collecitionview  <--  tabPage ( homepage & webview)

struct TabPageView: View {
    
    @State var page: WebPage
//    @State var showWebview = true

    @State var homeview = HomePage()

    @StateObject var webViewStore = WebViewStore()

    var  body: some View {
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
                print(Unmanaged.passUnretained(self.webViewStore).toOpaque())
                self.webViewStore.webView.load(URLRequest(url: URL(string: page.openedUrl ?? "163.com")!))
            }
            
            if page.openedUrl != nil{
                homeview.hidden()
            }else{
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

struct HomePage: View{
    var body: some View{
//        Color.clear
        ScrollView(.vertical){
            VStack{
                HStack{
                    Text("HomepageHeader")
                    Spacer()
                }
                HStack{
                    Text("Homepage")
                }
                Spacer()

                Text("HomepageFooter")
                Spacer()

            }
        }.background(.yellow)
            
    }
}


struct TabPageView_Previews: PreviewProvider {
    static var previews: some View {
        Text("problem")
    }
}
