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
    @EnvironmentObject var addressBar: AddressBarState
    @EnvironmentObject var webcacheStore: WebCacheStore
    @EnvironmentObject var dragScale: WndDragScale

    var index: Int
    var webCache: WebCache { webcacheStore.cache(at: index) }
    @ObservedObject var webWrapper: WebWrapper

    @State private var snapshotHeight: CGFloat = 0
    private var isVisible: Bool { index == selectedTab.curIndex }
    var body: some View {
        GeometryReader { geo in
            ZStack {
                if webCache.shouldShowWeb {
                    webComponent
                }

                if !webCache.shouldShowWeb {
                    Color.bkColor.overlay {
                        HomePageView()
                    }
                }
            }
            .onChange(of: openingLink.clickedLink, perform: { link in
                guard link != emptyURL else { return }

                print("clickedLink has changed: \(link)")
                let webcache = webcacheStore.cache(at: selectedTab.curIndex)
                webcache.lastVisitedUrl = link
                if webcache.shouldShowWeb{
                    webWrapper.webView.load(URLRequest(url: link))
                }else{
                    webcache.shouldShowWeb = true
                }
                openingLink.clickedLink = emptyURL
            })
            .onAppear {
                print("tabPage rect: \(geo.frame(in: .global))")
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
                if isVisible, !shouldExpand { // 截图，为缩小动画做准备
                    self
                        .environmentObject(selectedTab).environmentObject(toolbarState).environmentObject(animation)
                        .environmentObject(openingLink).environmentObject(addressBar).environmentObject(webcacheStore)
                        .takeSnapshot(completion: { image in
                            printWithDate("has took a snapshot")
                            let scale = image.scale
                            let cropRect = CGRect(x: 0, y: safeAreaTopHeight * scale, width: screen_width * scale, height: (snapshotHeight - dragScale.addressbarHeight - toolBarH) * scale)
                            if let croppedCGImage = image.cgImage?.cropping(to: cropRect) {
                                let croppedImage = UIImage(cgImage: croppedCGImage)
                                animation.snapshotImage = croppedImage
                                webCache.snapshotUrl = UIImage.createLocalUrl(withImage: croppedImage, imageName: webCache.id.uuidString)
                            }
                            if animation.progress == .obtainedCellFrame {
                                animation.progress = .startShrinking
                                printWithDate("progress: start Shrinking in tabpage")
                            } else {
                                animation.progress = .obtainedSnapshot
                                printWithDate("progress: obtained Snapshot")
                            }
                        })
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
                    webcacheStore.saveCaches()
                    if !TracelessMode.shared.isON {
                        let manager = HistoryCoreDataManager()
                        let history = LinkRecord(link: webCache.lastVisitedUrl.absoluteString, imageName: webCache.webIconUrl.absoluteString, title: webCache.title, createdDate: Date().milliStamp)
                        manager.insertHistory(history: history)
                    }
                }
            }
            .onChange(of: addressBar.needRefreshOfIndex, perform: { refreshIndex in
                if refreshIndex == index {
                    webWrapper.webView.reload()
                    addressBar.needRefreshOfIndex = -1
                }
            })

            .onReceive(addressBar.$stopLoadingOfIndex) { stopIndex in
                if stopIndex == index {
                    webWrapper.webView.stopLoading()
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
