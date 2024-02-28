//
//  TabPageView.swift
//  TableviewDemo
//
//  Created by ui06 on 4/16/23.
//

import Combine
import SwiftUI
import WebKit

struct TabPageView: View {
    @EnvironmentObject var animation: ShiftAnimation
    @EnvironmentObject var toolbarState: ToolBarState
    @Environment(OpeningLink.self) var openingLink
    @EnvironmentObject var addressBar: AddressBarState

    @Environment(WndDragScale.self) var dragScale
    @Environment(\.colorScheme) var colorScheme

    var webCache: WebCache
    @ObservedObject var webWrapper: WebWrapper
    let isVisible: Bool
    var doneLoading: (WebCache) -> Void
    @State private var shouldShowWeb = false

    var body: some View {
        GeometryReader { geo in
            content
                .onChange(of: openingLink.clickedLink) { _, link in
                    guard link != emptyURL else { return }
                    if isVisible {
                        webCache.lastVisitedUrl = link
                        if webCache.isWebVisible {
                            webWrapper.webMonitor.isLoadingDone = false
                            webWrapper.webView.load(URLRequest(url: link))
                        }
                        openingLink.clickedLink = emptyURL
                        print("clickedLink has changed at index: \(link)")
                    }
                }

                .onChange(of: toolbarState.shouldExpand) { _, shouldExpand in
                    if isVisible, !shouldExpand { // 截图，为缩小动画做准备
                        animation.snapshotImage = UIImage.snapshotImage(from: .defaultSnapshotURL)
                        if webCache.isWebVisible {
                            webWrapper.webView.scrollView.showsVerticalScrollIndicator = false
                            webWrapper.webView.scrollView.showsHorizontalScrollIndicator = false
                            webWrapper.webView.takeSnapshot(with: nil) { image, _ in
                                webWrapper.webView.scrollView.showsVerticalScrollIndicator = true
                                webWrapper.webView.scrollView.showsHorizontalScrollIndicator = true
                                if let img = image {
                                    animation.snapshotImage = img
                                    webCache.snapshotUrl = UIImage.createLocalUrl(withImage: img, imageName: webCache.id.uuidString + webtag)
                                }
                                animation.progress = animation.progress == .obtainedCellFrame ? .startShrinking : .obtainedSnapshot
                            }
                        } else {
                            let toSnapView = content
                                .environment(\.colorScheme, colorScheme)
                                .frame(width: geo.size.width, height: geo.size.height)

                            let render = ImageRenderer(content: toSnapView)
                            render.scale = UIScreen.main.scale

                            let defSnapshotImage = colorScheme == .light ? lightSnapshotImage : darkSnapshotImage
                            animation.snapshotImage = render.uiImage ?? defSnapshotImage

                            if colorScheme == .light {
                                lightSnapshotImage = animation.snapshotImage
                            } else {
                                darkSnapshotImage = animation.snapshotImage
                            }

                            webCache.snapshotUrl = UIImage.createLocalUrl(withImage: animation.snapshotImage, imageName: webCache.id.uuidString + String(describing: colorScheme))
                            animation.progress = animation.progress == .obtainedCellFrame ? .startShrinking : .obtainedSnapshot
                        }
                    }
                }
        }
    }

    var content: some View {
        Group {
            if shouldShowWeb {
                webComponent
            } else {
                BlankTabView()
                    .opacity(addressBar.isFocused ? 0 : 1)
                    .environment(dragScale)
            }
        }
        .onChange(of: webCache.lastVisitedUrl, initial: true) { _, _ in
            shouldShowWeb = webCache.lastVisitedUrl != emptyURL
        }
    }

    var webComponent: some View {
        TabWebView(webView: webWrapper.webView)
            .environment(dragScale)
            .id(webWrapper.id)
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

            .onChange(of: webWrapper.estimatedProgress) { _, newValue in
                if newValue >= 1.0 {
                    doneLoading(webCache)
                }
            }
            .onChange(of: addressBar.needRefreshOfIndex) { _, _ in
                if isVisible {
                    webWrapper.webMonitor.isLoadingDone = false
                    webWrapper.webView.reload()
                    addressBar.needRefreshOfIndex = -1
                }
            }
            .onChange(of: addressBar.stopLoadingOfIndex) { _, _ in
                if isVisible {
                    webWrapper.webView.stopLoading()
                }
            }
    }
}

struct TabPageView_Previews: PreviewProvider {
    static var previews: some View {
        Text("problem")
    }
}
