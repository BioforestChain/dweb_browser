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
//    @EnvironmentObject var animation: Animation
    @ObservedObject var animation: Animation

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
            .onChange(of: webWrapper.canGoBack, perform: { canGoBack in
                if let index = WebWrapperMgr.shared.store.firstIndex(of: webWrapper), index == browser.selectedTabIndex{
                    tabState.canGoBack = canGoBack
                }
            })
            .onChange(of: webWrapper.canGoForward, perform: { canGoForward in
                if let index = WebWrapperMgr.shared.store.firstIndex(of: webWrapper), index == browser.selectedTabIndex{
                    tabState.canGoForward = canGoForward
                }
            })

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
            
            .onReceive(animation.$progress, perform: { progress in
                if progress == .initial, tabState.showTabGrid, !hasTook{
                    let index = WebWrapperMgr.shared.store.firstIndex(of: webWrapper)
                    if index == browser.selectedTabIndex{
                        if let image = self.environmentObject(browser).snapshot(){
                        print(image)
                            hasTook = true  //avoid a dead runloop
                            webCache.snapshotUrl = UIImage.createLocalUrl(withImage: image, imageName: webCache.id.uuidString)
                            animation.snapshotImage = image
                            animation.progress = .startShrinking
                            DispatchQueue.main.asyncAfter(deadline: .now()+0.1, execute: {hasTook = false}) // reset the state var once this time animation
                        }
                    }
                }
            })
            .onChange(of: tabState.goForwardTapped) { tapped in
                if tapped{
                    goForward()
                    tabState.goForwardTapped = false
                }
            }
            .onChange(of: tabState.goBackTapped) { tapped in
                if tapped{
                    goBack()
                    tabState.goBackTapped = false
                }
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
