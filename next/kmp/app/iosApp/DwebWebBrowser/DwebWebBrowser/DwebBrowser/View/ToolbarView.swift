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

    @EnvironmentObject var toolbarState: ToolBarState
    @EnvironmentObject var openingLink: OpeningLink
    @EnvironmentObject var addressBar: AddressBarState
    @Environment(WndDragScale.self) var dragScale

    @ObservedObject var webMonitor: WebMonitor

    @State private var loadingDone: Bool = false
    private var isShowingWeb: Bool { webcacheStore.cache(at: seletecdTab.index).isWebVisible }
    private var canCreateDesktopLink: Bool { isShowingWeb && loadingDone }
    
    var body: some View {
        Group {
            if toolbarState.shouldExpand {
                fiveButtons
            } else {
                threeButtons
            }
        }
        .frame(height: addressBar.isFocused ? 0 : dragScale.properValue(max: maxToolBarH))
        .opacity(addressBar.isFocused ? 0 : 1)
    }
    
    var threeButtons: some View {
        ZStack {
            GeometryReader { geo in
                let size = geo.size
                HStack(alignment: .center) {
                    Spacer().frame(width: size.width / 15)
                    
                    BiColorButton(imageName: "add") {
                        toolbarState.createTabTapped = true
                    }
                    
                    Spacer()
                    Text("\(webcacheStore.cacheCount)个标签页")
                        .foregroundColor(.primary)
                        .font(dragScale.scaledFont_18)
                        .fontWeight(.semibold)
                    
                    Spacer()
                    
                    Button {
                        toolbarState.shouldExpand = true
                    } label: {
                        Text("完成")
                            .foregroundColor(Color.primary)
                            .font(dragScale.scaledFont_18)
                            .fontWeight(.semibold)
                    }
                    
                    Spacer().frame(width: size.width / 15)
                }
            }
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
            .onReceive(webMonitor.$isLoadingDone) { done in
                loadingDone = done
            }
            .disabled(!canCreateDesktopLink)
                
            Spacer()
            if canCreateDesktopLink {
                BiColorButton(imageName: "add") {
                    toolbarState.createTabTapped = true
                }
            } else {
                BiColorButton(imageName: "scan") {
                    doScan()
                }
            }
            Spacer()
            BiColorButton(imageName: "shift") {
                Log("shift tab was clicked")
                toolbarState.shouldExpand = false
            }
            Spacer()
            BiColorButton(imageName: "more") {
                withAnimation {
                    toolbarState.showMoreMenu = true
                }
            }
            Spacer()
        }
        .clipped()
        .sheet(isPresented: $toolbarState.isPresentingScanner) {
            CodeScannerView(codeTypes: [.qr], showViewfinder: true, completion: scanCompletion)
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
                self.toolbarState.isPresentingScanner = true
            }
        }
    }
    
    private func scanCompletion(response: Result<ScanResult, ScanError>) {
        toolbarState.isPresentingScanner = false
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
                                                  iconString: webCache.webIconUrl.absoluteString){ e in  }
        }
    }
}

