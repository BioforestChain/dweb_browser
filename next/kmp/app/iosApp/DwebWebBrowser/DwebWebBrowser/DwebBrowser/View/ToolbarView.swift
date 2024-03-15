//
//  ToolBarView.swift
//  TableviewDemo
//
//  Created by ui06 on 4/16/23.
//

import Combine
import SwiftUI
import UIKit

struct ToolbarView: View {
    @Environment(SelectedTab.self) var seletecdTab
    @Environment(WebCacheStore.self) var webcacheStore

    @Environment(ToolBarState.self) var toolbarState
    @Environment(OpeningLink.self) var openingLink
    @Environment(AddressBarState.self) var addressBar
    @Environment(WndDragScale.self) var dragScale
    @Environment(WebMonitor.self) var webMonitor

    @State private var loadingDone: Bool = false
    @State private var isScanning: Bool = false
    @State private var tabExpanded: Bool = true

    private var isShowingWeb: Bool { webcacheStore.cache(at: seletecdTab.index).isWebVisible }
    private var canCreateDesktopLink: Bool { isShowingWeb && loadingDone }
    
    var body: some View {
        Group {
            if tabExpanded {
                fiveButtons
            } else {
                threeButtons
            }
        }
        .frame(height: addressBar.isFocused ? 0 : dragScale.properValue(max: maxToolBarH))
        .accessibilityElement(children: .contain)
        .accessibilityIdentifier("ToolbarView")
        .opacity(addressBar.isFocused ? 0 : 1)
        .onChange(of: webMonitor.isLoadingDone) { _, isDone in
            loadingDone = isDone
        }
        .onChange(of: toolbarState.tabsState) { _, state in
            if state == .expanded || state == .shrinked {
                withAnimation(.snappy){
                    tabExpanded = state == .expanded
                }
            }
        }
    }
    
    var threeButtons: some View {
        HStack {
            BiColorButton(imageName: "add") {
                toolbarState.shouldCreateTab = true
            }
            .accessibilityAddTraits(.isButton)
            .accessibilityIdentifier("add")
            .padding(.leading, screen_width * dragScale.onWidth / 15)
                        
            Spacer()
            Text("\(webcacheStore.cacheCount)个标签页")
                .foregroundColor(.primary)
                .font(dragScale.scaledFont_18)
                .fontWeight(.semibold)
                        
            Spacer()
                        
            Button {
                toolbarState.tabsState = .shouldExpand
            } label: {
                Text("完成")
                    .foregroundColor(Color.primary)
                    .font(dragScale.scaledFont_18)
                    .fontWeight(.semibold)
            }
            .accessibilityAddTraits(.isButton)
            .accessibilityIdentifier("done")
            .padding(.trailing, screen_width * dragScale.onWidth / 15)
        }
    }
    
    var fiveButtons: some View {
        HStack(alignment: .center) {
            Spacer()
                
            Button(action: {
                creatDesktopLink()
            }) {
                plusImage
            }
            .accessibilityIdentifier("shortcut")
            .disabled(!canCreateDesktopLink)
                
            Spacer()
            if canCreateDesktopLink {
                BiColorButton(imageName: "add") {
                    toolbarState.shouldCreateTab = true
                }
                .accessibilityAddTraits(.isButton)
                .accessibilityIdentifier("add")
            } else {
                BiColorButton(imageName: "scan") {
                    doScan()
                }
                .accessibilityAddTraits(.isButton)
                .accessibilityIdentifier("scan")
            }
            Spacer()
            BiColorButton(imageName: "shift") {
                toolbarState.tabsState = .shouldShrink
            }
            .accessibilityAddTraits(.isButton)
            .accessibilityIdentifier("shift")
            Spacer()
            BiColorButton(imageName: "more") {
                withAnimation {
                    toolbarState.showMoreMenu = true
                }
            }
            .accessibilityAddTraits(.isButton)
            .accessibilityIdentifier("more")
            Spacer()
        }
        
        .clipped()
        .sheet(isPresented: $isScanning) {
            CodeScannerView(codeTypes: [.qr], showViewfinder: true, completion: scanCompletion)
        }
        .onChange(of: isScanning) { _, isPresenting in
            toolbarState.isPresentingScanner = isPresenting
        }
    }
    
    var plusImage: some View {
        Image(systemName: "apps.iphone.badge.plus")
            .renderingMode(.template)
            .resizable()
            .aspectRatio(contentMode: .fit)
            .background(Color.bk)
            .foregroundColor(canCreateDesktopLink ? .primary : .gray)
            .frame(width: dragScale.toolbarItemWidth, height: dragScale.toolbarItemWidth)
    }
    
    private func doScan() {
        Log("scan qrcode")
        browserViewDataSource.requestCameraPermission { result in
            Log("\(result)")
            guard result else { return }
            DispatchQueue.main.async {
                isScanning = true
            }
        }
    }
    
    private func scanCompletion(response: Result<ScanResult, ScanError>) {
        isScanning = false
        if case let .success(result) = response {
            Log(result.string)
            let url = URL(string: result.string)
            if url?.scheme == "dweb" {
                browserViewDelegate.openDeepLink(url: result.string)
            } else {
                addressBar.inputText = result.string
                let url = URL.createUrl(addressBar.inputText)
                DispatchQueue.main.async {
                    openingLink.clickedLink = url
                    addressBar.isFocused = false
                }
            }
        }
    }
    
    private func creatDesktopLink() {
        Task {
            let webCache = webcacheStore.cache(at: seletecdTab.index)
            browserViewDelegate.createDesktopLink(link: webCache.lastVisitedUrl.absoluteString,
                                                  title: webCache.title,
                                                  iconString: webCache.webIconUrl.absoluteString) { _ in }
        }
    }
}
