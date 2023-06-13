//
//  TabPageView.swift
//  TableviewDemo
//
//  Created by ui06 on 4/16/23.
//

import SwiftUI
import Combine

struct TabPageView: View {
    @ObservedObject var webCache: WebCache
    @ObservedObject var webWrapper: WebWrapper
    @ObservedObject var toolbarState: ToolBarState
    
    @EnvironmentObject var selectedTab: SelectedTab
    //    @EnvironmentObject var animation: Animation
    @ObservedObject var animation: Animation
    
    @State var homeview = HomeView()
    @State var hasTook = false
    var  body: some View {
        ZStack{
            HomePageView()
            if webCache.lastVisitedUrl != testURL {
                
                WebView(webView: webWrapper.webView, url: webCache.lastVisitedUrl)
                    .onChange(of: webWrapper.canGoBack, perform: { canGoBack in
                        if let index = WebWrapperMgr.shared.store.firstIndex(of: webWrapper), index == selectedTab.curIndex{
                            toolbarState.canGoBack = canGoBack
                        }
                    })
                    .onChange(of: webWrapper.canGoForward, perform: { canGoForward in
                        if let index = WebWrapperMgr.shared.store.firstIndex(of: webWrapper), index == selectedTab.curIndex{
                            toolbarState.canGoForward = canGoForward
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
                            if TraceLessMode.shared.isON{
                                let manager = HistoryCoreDataManager()
                                let history = LinkRecord(link: webCache.lastVisitedUrl.absoluteString, imageName: webCache.webIconUrl.absoluteString, title: webCache.title, createdDate: Date().milliStamp)
                                manager.insertHistory(history: history)
                            }
                        }
                    }
                
                    .onChange(of: toolbarState.goForwardTapped) { tapped in
                        if tapped{
                            goForward()
                            toolbarState.goForwardTapped = false
                        }
                    }
                    .onChange(of: toolbarState.goBackTapped) { tapped in
                        if tapped{
                            goBack()
                            toolbarState.goBackTapped = false
                        }
                    }
            }
        }
        
        .onReceive(animation.$progress, perform: { progress in
            if progress == .initial, toolbarState.showTabGrid, !hasTook{
                let index = WebWrapperMgr.shared.store.firstIndex(of: webWrapper)
                if index == selectedTab.curIndex{
                    if let image = self.environmentObject(selectedTab).snapshot(){
                        print(image)
                        let scale = image.scale
                        let cropRect = CGRect(x: 0, y: 0, width: screen_width * scale, height: 788.666 * scale)
                        if let croppedCGImage = image.cgImage?.cropping(to: cropRect) {
                            let croppedImage = UIImage(cgImage: croppedCGImage)
                            animation.snapshotImage = croppedImage
                        }
                        hasTook = true  //avoid a dead run loop
                        webCache.snapshotUrl = UIImage.createLocalUrl(withImage: image, imageName: webCache.id.uuidString)
                        animation.progress = .startShrinking
                        DispatchQueue.main.asyncAfter(deadline: .now()+0.1){hasTook = false} // reset the state var once this time animation
                    }
                }
            }
        })
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
