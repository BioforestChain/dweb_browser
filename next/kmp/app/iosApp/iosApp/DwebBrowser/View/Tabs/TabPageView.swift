//
//  TabPageView.swift
//  TableviewDemo
//
//  Created by ui06 on 4/16/23.
//

import Combine
import SwiftUI
import DwebShared

struct TabPageView: View {
    @EnvironmentObject var animation: ShiftAnimation
    @EnvironmentObject var toolbarState: ToolBarState
    @EnvironmentObject var selectedTab: SelectedTab
    @EnvironmentObject var openingLink: OpeningLink
    @EnvironmentObject var addressBar: AddressBarState
    @EnvironmentObject var webcacheStore: WebCacheStore
    @EnvironmentObject var dragScale: WndDragScale
    @EnvironmentObject var browerArea: BrowserArea
    @Environment(\.colorScheme) var colorScheme
    
    var tabIndex: Int { webcacheStore.index(of: webCache)! }
    var webCache: WebCache
    @ObservedObject var webWrapper: WebWrapper

    private let screemScale = UIScreen.main.scale
    
    @State private var snapshotHeight: CGFloat = 0
    private var isVisible: Bool { tabIndex == selectedTab.curIndex }
    var body: some View {
        GeometryReader { geo in
            content
            .onChange(of: openingLink.clickedLink) { _, link in
                guard link != emptyURL else { return }

                print("clickedLink has changed: \(link)")
                let webcache = webcacheStore.cache(at: selectedTab.curIndex)
                webcache.lastVisitedUrl = link
                if webcache.shouldShowWeb {
                    webWrapper.webView.load(URLRequest(url: link))
                } else {
                    webcache.shouldShowWeb = true
                }
                openingLink.clickedLink = emptyURL
            }
            .onAppear {
                print("tabPage rect: \(geo.frame(in: .global))")
                snapshotHeight = geo.frame(in: .global).height
            }
            .onChange(of: toolbarState.goForwardTapped) { _, tapped in
                if tapped {
                    goForward()
                    toolbarState.goForwardTapped = false
                }
            }
            .onChange(of: toolbarState.goBackTapped) { _, tapped in
                if tapped {
                    goBack()
                    toolbarState.goBackTapped = false
                }
            }

            .onChange(of: toolbarState.shouldExpand) { _, shouldExpand in
                if isVisible, !shouldExpand { // 截图，为缩小动画做准备
                        animation.snapshotImage = UIImage.snapshotImage(from: .defaultSnapshotURL)
                        if webCache.shouldShowWeb {
                            webWrapper.webView.scrollView.showsVerticalScrollIndicator = false
                            webWrapper.webView.scrollView.showsHorizontalScrollIndicator = false
                            webWrapper.webView.takeSnapshot(with: nil) { image, _ in
                                webWrapper.webView.scrollView.showsVerticalScrollIndicator = true
                                webWrapper.webView.scrollView.showsHorizontalScrollIndicator = true
                                if let img = image {
                                    animation.snapshotImage = img
                                    webCache.snapshotUrl = UIImage.createLocalUrl(withImage: img, imageName: webCache.id.uuidString)
                                }
                                animation.progress = animation.progress == .obtainedCellFrame ? .startShrinking : .obtainedSnapshot
                            }
                        } else {
                            let toSnapView = content
                                .environment(\.colorScheme, colorScheme)
                                .frame(width: geo.size.width, height: geo.size.height)
                            let render = ImageRenderer(content: toSnapView)
                            render.scale = screemScale
                            animation.snapshotImage = render.uiImage ?? UIImage.snapshotImage(from: .defaultSnapshotURL)
                            webCache.snapshotUrl = UIImage.createLocalUrl(withImage: animation.snapshotImage, imageName: webCache.id.uuidString)
                            animation.progress = animation.progress == .obtainedCellFrame ? .startShrinking : .obtainedSnapshot
                        }
                    }
            }
        }
    }
    
    var content: some View {
        ZStack {
            if webCache.shouldShowWeb {
                webComponent
            } else {
                Color.bkColor.overlay {
                    HomePageView()
                        .environmentObject(dragScale)
                }
            }
        }
    }

    var webComponent: some View {
        TabWebView(webView: webWrapper.webView)
            .onAppear {
                if webWrapper.estimatedProgress < 0.001 {
                    webWrapper.webView.load(URLRequest(url: webCache.lastVisitedUrl))
                }
            }
            .onChange(of: webWrapper.url) { _, newValue in
                if let validUrl = newValue, webCache.lastVisitedUrl != validUrl {
                    webCache.lastVisitedUrl = validUrl
                }
            }
            .onChange(of: webWrapper.title) { _, newValue in
                if let validTitle = newValue {
                    webCache.title = validTitle
                }
            }
            .onChange(of: webWrapper.icon) { _, icon in
                webCache.webIconUrl = URL(string: String(icon)) ?? .defaultWebIconURL
            }
            .onChange(of: webWrapper.canGoBack) { _, canGoBack in
                if isVisible {
                    toolbarState.canGoBack = canGoBack
                }
                isCanCloseApp = !canGoBack
            }
            .onChange(of: webWrapper.canGoForward) { _, canGoForward in
                if isVisible {
                    toolbarState.canGoForward = canGoForward
                }
            }
            .onChange(of: webWrapper.estimatedProgress) { _, newValue in
                if newValue >= 1.0 {
                    webcacheStore.saveCaches()
                    if !TracelessMode.shared.isON {
                        DwebBrowserHistoryStore.shared.addHistoryRecord(title: webCache.title, url: webCache.lastVisitedUrl.absoluteString)
                    }
                }
            }
            .onChange(of: addressBar.needRefreshOfIndex) { _, refreshIndex in
                if refreshIndex == tabIndex {
                    webWrapper.webView.reload()
                    addressBar.needRefreshOfIndex = -1
                }
            }
            .onChange(of: addressBar.stopLoadingOfIndex) { _, stopIndex in
                if stopIndex == tabIndex {
                    webWrapper.webView.stopLoading()
                }
            }
            .onChange(of: toolbarState.creatingDesktopLink) { _, isCreating in
                if isCreating{
                    Task{
                        try await DwebBrowserIosSupport().browserService.createDesktopLink(link: webCache.lastVisitedUrl.absoluteString, title: webCache.title, iconString: webCache.webIconUrl.absoluteString)
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
