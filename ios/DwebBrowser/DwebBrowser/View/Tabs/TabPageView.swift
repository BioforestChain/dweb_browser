//
//  TabPageView.swift
//  TableviewDemo
//
//  Created by ui06 on 4/16/23.
//

import SwiftUI
import Combine
//层级关系  最前<-- 快照(缩放动画）<-- collecitionview  <--  tabPage ( homepage & webview)

struct TabPageView: View {
    @ObservedObject var webCache: WebCache
    @ObservedObject var webWrapper: WebWrapper
    
    @ObservedObject var tabState: TabState
    @EnvironmentObject var browser: BrowerVM

    @State var homeview = HomeView()
    @State var hasTook = false
    var  body: some View {
        ZStack{
            NavigationView {
                WebView(webView: webWrapper.webView, url: webCache.lastVisitedUrl)
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
//                    .background(.red)
            }
            .onAppear {
                print("webWrapper.webView ---- \(webWrapper.webView)")
                
            }

            .onChange(of: webWrapper.estimatedProgress){ newValue in
                if newValue >= 1.0{
                    WebCacheMgr.shared.saveCaches()
                }
            }
            
            .onChange(of: webWrapper.title!) { title in
                    webCache.title = title
            }
            .onChange(of: webWrapper.url) { visitingUrl in
                if let url = visitingUrl{
                    webCache.lastVisitedUrl = url
                }
            }
            .onChange(of: WebWrapperMgr.shared.wrapperStore.count) { newValue in
                print("web count is " ,newValue)
            }
            
            .onChange(of: tabState.showTabGrid, perform: { showTabGrid in
                if showTabGrid, !hasTook {
                    let index = WebWrapperMgr.shared.wrapperStore.firstIndex(of: webWrapper)
                    if index == browser.selectedTabIndex{
                        if let image = self.environmentObject(browser).snapshot(){
                        print(image)
                            hasTook = true  //avoid a dead runloop
                            webCache.snapshotUrl = UIImage.createLocalUrl(withImage: image, imageName: webCache.id.uuidString)
                            browser.currentSnapshotImage = image
                            DispatchQueue.main.asyncAfter(deadline: .now()+0.5, execute: {hasTook = false}) // reset the state var once this time animation

                        }
                    }
                }
            })
            
//            .onReceive(tabState.$showTabGrid) { showDeck in
//                if showDeck, !hasTook {
//                    let index = WebWrapperMgr.shared.wrapperStore.firstIndex(of: webWrapper)
//                    if index == browser.selectedTabIndex{
//                        if let image = self.environmentObject(browser).snapshot(){
//                        print(image)
//                            hasTook = true  //avoid a dead runloop
//                            webCache.snapshotUrl = UIImage.createLocalUrl(withImage: image, imageName: webCache.id.uuidString)
//                            browser.currentSnapshotImage = image
//                            DispatchQueue.main.asyncAfter(deadline: .now()+0.5, execute: {hasTook = false}) // reset the state var once this time animation
//
//                        }
//                    }
//                }
//            }
            
            if webCache.lastVisitedUrl == nil{
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
