//
//  TabPageView.swift
//  TableviewDemo
//
//  Created by ui06 on 4/16/23.
//

import SwiftUI

//层级关系  最前<-- 快照(缩放动画）<-- collecitionview  <--  tabPage ( homepage & webview)


struct TabPageView: View {
    
    @ObservedObject var webWrapper: WebWrapper
    @EnvironmentObject var browser: BrowerVM

    @State var homeview = HomeView()
    @State var hasTook = false
    var  body: some View {
        ZStack{
            NavigationView {
                WebView(webView: webWrapper.webView, url: webWrapper.webCache.lastVisitedUrl)
                    .navigationBarTitle(Text(verbatim: webWrapper.title ?? ""), displayMode: .inline)
                    .navigationBarItems(trailing: HStack {
                        Button(action: goBack) {
                            Image(systemName: "chevron.left")
                                .imageScale(.large)
                                .aspectRatio(contentMode: .fit)
                                .frame(width: 32, height: 32)
                        }.disabled(!webWrapper.canGoBack)
                        Button(action: goForward) {
                            Image(systemName: "chevron.right")
                                .imageScale(.large)
                                .aspectRatio(contentMode: .fit)
                                .frame(width: 32, height: 32)
                        }.disabled(!webWrapper.canGoForward)
                    })
                    .background(.red)
            }.onAppear {
//                print("webWrapper.webView ---- \(webWrapper.webView)")
            }
            .onChange(of: webWrapper.webView.url) { visitingUrl in
                if let url = visitingUrl{
                    webWrapper.webCache.lastVisitedUrl = url
                }
            }
            .onChange(of: webWrapper.webView.estimatedProgress) { progress in
                if progress >= 1.0{
                    print("caching \(webWrapper.webCache)")
                    browser.saveCaches()
                }
            }
            .onReceive(browser.$showingOptions) { showDeck in
                if showDeck, !hasTook {
                    let index = browser.pages.map({$0.webWrapper}).firstIndex(of: webWrapper)
                    if index == browser.selectedTabIndex{
                        if let image = self.environmentObject(browser).snapshot(){
//                                            let rect = geo.frame(in: .global)
                        print(image)
                            hasTook = true
                            browser.currentSnapshotImage = image
                            DispatchQueue.main.asyncAfter(deadline: .now()+0.5, execute: {hasTook = false})
//                                        if let image = self.takeScreenshot(of:rect) {
//                                            if let image = self.snapshot() {

//                                            browser.capturedImage = image
                            
                        }
                    }
                }
            }
            
            if webWrapper.webCache.lastVisitedUrl == nil{
                homeview
            }
        }
    }
    
    func goBack() {
        webWrapper.webView.goBack()
    }
    
    func goForward() {
        webWrapper.webView.goForward()
    }
}

struct TabPageView_Previews: PreviewProvider {
    static var previews: some View {
        Text("problem")
    }
}
