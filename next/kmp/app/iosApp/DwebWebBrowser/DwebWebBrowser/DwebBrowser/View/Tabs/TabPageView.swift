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
    @Environment(ShiftAnimation.self) var animation
    @Environment(ToolBarState.self) var toolbarState
    @Environment(OpeningLink.self) var openingLink
    @Environment(AddressBarState.self) var addressBar
    @Environment(WndDragScale.self) var dragScale
    @Environment(\.colorScheme) var colorScheme
    
    var webCache: WebCache
    @ObservedObject var webWrapper: WebWrapper
    let isVisible: Bool
    var doneLoading: (WebCache) -> Void
    @State private var shouldShowWeb = false
    @State var draggingSize: CGSize = .zero
    @State var menuIndex = 1
    @State var menuActionIndex = -1
    @Binding var menuAction: DragDownMenuAction
    var body: some View {
        GeometryReader { geo in
            contentView
                .onChange(of: toolbarState.tabsState) { _, state in
                    handleAnimationStateChange(state: state, in: geo.size)
                }
                .onChange(of: menuIndex) { oldValue, newValue in
                    print("")
                }
        }
        .accessibilityElement(children: .contain)
        .accessibilityIdentifier("TabPageView")
    }
    
    var contentView: some View {
        Group {
            if shouldShowWeb {
                webContentView
            } else {
                BlankTabView()
                    .opacity(addressBar.isFocused ? 0 : 1)
                    .environment(dragScale)
            }
        }
        .onAppear { updateShouldShowWeb() }
        .onChange(of: webCache.lastVisitedUrl, initial: true) { _, _ in updateShouldShowWeb() }
        .onChange(of: openingLink.clickedLink) { _, link in handleOpeningLinkChange(link) }
    }
    
    var webContentView: some View {
        ZStack(alignment: .top) {
            TabWebView(webView: webWrapper.webView,
                       xOffset: $draggingSize.width,
                       yOffset: $draggingSize.height,
                       menuIndex: $menuIndex,
                       actionIndex: $menuActionIndex
            )
            .environment(dragScale)
            .id(webWrapper.id)
            .offset(y: draggingSize.height)
            
            .onAppear { loadWebViewIfNeeded() }
            .onChange(of: webWrapper.url) { _, newValue in updateWebCacheUrl(newValue) }
            .onChange(of: webWrapper.title) { _, newValue in updateWebCacheTitle(newValue) }
            .onChange(of: webWrapper.icon) { _, icon in updateWebCacheIcon(icon) }
            .onChange(of: webWrapper.estimatedProgress) { _, newValue in handleLoadingProgress(newValue) }
            .onChange(of: addressBar.needRefreshOfIndex) { _, _ in handleRefresh() }
            .onChange(of: addressBar.stopLoadingOfIndex) { _, _ in handleStopLoading() }
            .onChange(of: menuActionIndex) { _, index in choseAction(index) }
            
            ZStack {
                DragMenuView(dragSize: $draggingSize, selectedIndex: menuIndex)
                    .opacity(draggingSize.height > minYOffsetToSelectAction ? 1.0 : 0)
            }
            .frame(height: draggingSize.height * 1.2)
        }
    }
}

// MARK: - Helper Functions
extension TabPageView {
    private func updateShouldShowWeb() {
        shouldShowWeb = webCache.lastVisitedUrl != emptyURL
    }
    
    private func handleOpeningLinkChange(_ link: URL) {
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
    
    private func loadWebViewIfNeeded() {
        if webWrapper.estimatedProgress < 0.001 {
            webWrapper.webView.load(URLRequest(url: webCache.lastVisitedUrl))
        }
    }
    
    private func updateWebCacheUrl(_ newValue: URL?) {
        if let validUrl = newValue, webCache.lastVisitedUrl != validUrl {
            webCache.lastVisitedUrl = validUrl
        }
    }
    
    private func updateWebCacheTitle(_ newValue: String?) {
        if let validTitle = newValue {
            webCache.title = validTitle
        }
    }
    
    private func updateWebCacheIcon(_ icon: NSString) {
        webCache.webIconUrl = URL(string: String(icon)) ?? .defaultWebIconURL
        //        webCache.webIconUrl = URL(string: icon) ?? .defaultWebIconURL
    }
    
    private func handleLoadingProgress(_ newValue: Double) {
        if newValue >= 1.0 {
            doneLoading(webCache)
        }
    }
    
    private func handleRefresh() {
        if isVisible {
            webWrapper.webMonitor.isLoadingDone = false
            webWrapper.webView.reload()
            addressBar.needRefreshOfIndex = -1
        }
    }
    
    private func handleStopLoading() {
        if isVisible {
            webWrapper.webView.stopLoading()
        }
    }
    
    private func choseAction(_ index: Int) {
        if index == 0 {
            menuAction = .createNewTab
            print("add new tab ")
        } else if index == 1 {
            menuAction = .refreshTab
            print("refresh tab ")
        } else if index == 2 {
            menuAction = .closeTab
            print("close tab ")
        }
    }
    
    private func handleAnimationStateChange(state: ToolBarState.TabsStates, in size: CGSize) {
        if isVisible, state == .shouldShrink {
            prepareForShrinkingAnimation(in: size)
        }
    }
    
    private func prepareForShrinkingAnimation(in size: CGSize) {
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
                updateAnimationProgress()
            }
        } else {
            takeSnapshotUsingImageRenderer(in: size)
        }
    }
    
    private func updateAnimationProgress() {
        animation.progress = animation.progress == .obtainedCellFrame ? .startShrinking : .obtainedSnapshot
    }
    
    private func takeSnapshotUsingImageRenderer(in size: CGSize) {
        let snapshotView = contentView
            .environment(\.colorScheme, colorScheme)
            .frame(width: size.width, height: size.height)
        
        let render = ImageRenderer(content: snapshotView)
        render.scale = UIScreen.main.scale
        
        let defaultSnapshotImage = colorScheme == .light ? lightSnapshotImage : darkSnapshotImage
        animation.snapshotImage = render.uiImage ?? defaultSnapshotImage
        
        if colorScheme == .light {
            lightSnapshotImage = animation.snapshotImage
        } else {
            darkSnapshotImage = animation.snapshotImage
        }
        
        webCache.snapshotUrl = UIImage.createLocalUrl(withImage: animation.snapshotImage, imageName: webCache.id.uuidString + String(describing: colorScheme))
        updateAnimationProgress()
    }
}
struct TabPageView_Previews: PreviewProvider {
    static var previews: some View {
        Text("problem")
    }
}
