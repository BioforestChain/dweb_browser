//
//  TabPageView.swift
//  TableviewDemo
//
//  Created by ui06 on 4/16/23.
//

import Combine
import SwiftUI

struct TabPageView: View {
    @EnvironmentObject var animation: ShiftAnimation
    @EnvironmentObject var toolbarState: ToolBarState
    @EnvironmentObject var selectedTab: SelectedTab
    @EnvironmentObject var openingLink: OpeningLink
    
    var index: Int
    var webCache: WebCache { WebCacheMgr.shared.store[index] }
    var webWrapper: WebWrapper { WebWrapperMgr.shared.store[index] }
    
    @State private var snapshotHeight: CGFloat = 0

    private var isVisible: Bool { let index = WebWrapperMgr.shared.store.firstIndex(of: webWrapper); return index == selectedTab.curIndex }
    var body: some View {
        GeometryReader { geo in
            ZStack {
                if webCache.shouldShowWeb {
                    webComponent
                } else {
                    HomePageView()
                }
            }
            .onAppear {
                snapshotHeight = geo.frame(in: .global).height
            }
        }

        .onChange(of: toolbarState.shouldExpand) { shouldExpand in
            if shouldExpand { // 准备放大动画
                animation.snapshotImage = UIImage.snapshotImage(from: WebCacheMgr.shared.store[selectedTab.curIndex].snapshotUrl)
                animation.progress = .startExpanding
            }
            
            if !shouldExpand { // 截图，为缩小动画做准备
                let index = WebWrapperMgr.shared.store.firstIndex(of: webWrapper)
                if index == selectedTab.curIndex {
                    if let image = self
                        .environmentObject(selectedTab)
                        .environmentObject(toolbarState)
                        .environmentObject(animation)
                        .environmentObject(openingLink).snapshot()
                    {
                        let scale = image.scale
                        let cropRect = CGRect(x: 0, y: 0, width: screen_width * scale, height: snapshotHeight * scale)
                        if let croppedCGImage = image.cgImage?.cropping(to: cropRect) {
                            let croppedImage = UIImage(cgImage: croppedCGImage)
                            animation.snapshotImage = croppedImage
                            webCache.snapshotUrl = UIImage.createLocalUrl(withImage: croppedImage, imageName: webCache.id.uuidString)
                        }
                        if animation.progress == .obtainedCellFrame {
                            animation.progress = .startShrinking
                            printWithDate(msg: "startShrinking in obtainedSnapshot")
                        } else {
                            animation.progress = .obtainedSnapshot
                        }
                    }
                }
            }
        }
    }
    
    var webComponent: some View {
        WebView(webView: webWrapper.webView, url: webCache.lastVisitedUrl)
            .onChange(of: webWrapper.canGoBack, perform: { canGoBack in
                if isVisible {
                    toolbarState.canGoBack = canGoBack
                }
            })
            .onChange(of: webWrapper.canGoForward, perform: { canGoForward in
                if isVisible {
                    toolbarState.canGoForward = canGoForward
                }
            })
            .onChange(of: webWrapper.estimatedProgress) { newValue in
                if newValue >= 1.0 {
                    WebCacheMgr.shared.saveCaches()
                }
            }
        
            .onChange(of: webWrapper.title!) { title in
                webCache.title = title
            }
            .onChange(of: webWrapper.icon) { icon in
                webCache.webIconUrl = URL(string: String(icon)) ?? .defaultWebIconURL
            }
            .onChange(of: webWrapper.url) { visitingUrl in
                if let url = visitingUrl {
                    webCache.lastVisitedUrl = url
                    if !TraceLessMode.shared.isON {
                        let manager = HistoryCoreDataManager()
                        let history = LinkRecord(link: webCache.lastVisitedUrl.absoluteString, imageName: webCache.webIconUrl.absoluteString, title: webCache.title, createdDate: Date().milliStamp)
                        manager.insertHistory(history: history)
                    }
                }
            }
        
            .onChange(of: toolbarState.goForwardTapped) { tapped in
                if tapped {
                    goForward()
                    toolbarState.goForwardTapped = false
                }
            }
            .onChange(of: toolbarState.goBackTapped) { tapped in
                if tapped {
                    goBack()
                    toolbarState.goBackTapped = false
                }
            }

            .onChange(of: openingLink.clickedLink) { url in
                print("clickedLink has changed: \(url)")
                if isVisible {
                    webWrapper.webView.load(URLRequest(url: url))
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
