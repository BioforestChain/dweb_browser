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
    @ObservedObject var webWrapper: WebWrapper

    @State private var snapshotHeight: CGFloat = 0
    private var isVisible: Bool { let index = WebWrapperMgr.shared.store.firstIndex(of: webWrapper); return index == selectedTab.curIndex }
    var body: some View {
        GeometryReader { geo in
            ZStack {
                if webCache.shouldShowWeb{
                    webComponent
                }
                
                if !webCache.shouldShowWeb {
                    Color.bkColor.overlay {
                        HomePageView()
                    }
                }
            }
            .onAppear {
                snapshotHeight = geo.frame(in: .global).height
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
            .onChange(of: toolbarState.shouldExpand) { shouldExpand in
                if !shouldExpand { // 截图，为缩小动画做准备
                    let index = WebWrapperMgr.shared.store.firstIndex(of: webWrapper)
                    if index == selectedTab.curIndex {
                        if let image = self
                            .environmentObject(selectedTab).environmentObject(toolbarState).environmentObject(animation).environmentObject(openingLink)
                            .snapshot()
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
    }
    
    var webComponent: some View {
        WebView(webView: webWrapper.webView)
            .onAppear {
                if webWrapper.estimatedProgress < 0.001 {
                    webWrapper.webView.load(URLRequest(url: webCache.lastVisitedUrl))
                }
                print("onappear progress:\(webWrapper.webView.estimatedProgress)")
            }
            .onChange(of: openingLink.clickedLink) { url in
                print("clickedLink has changed: \(url)")
                guard url != emptyURL else { return }
                if isVisible {
                    webWrapper.webView.load(URLRequest(url: url))
                    
                }
                openingLink.clickedLink = emptyURL
            }
        
            .onChange(of: webWrapper.url) { url in
                if let validUrl = url, webCache.lastVisitedUrl != validUrl {
                    webCache.lastVisitedUrl = validUrl
                }
            }
            .onChange(of: webWrapper.title) { title in
                if let validTitle = title {
                    webCache.title = validTitle
                }
            }
            .onChange(of: webWrapper.icon) { icon in
                webCache.webIconUrl = URL(string: String(icon)) ?? .defaultWebIconURL
            }
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
                    if !TraceLessMode.shared.isON {
                        let manager = HistoryCoreDataManager()
                        let history = LinkRecord(link: webCache.lastVisitedUrl.absoluteString, imageName: webCache.webIconUrl.absoluteString, title: webCache.title, createdDate: Date().milliStamp)
                        manager.insertHistory(history: history)
                    }
                }
                printWithDate(msg: "in webComponent,url is: \(webWrapper.url), web index \(index), progress goes \(newValue)")
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
