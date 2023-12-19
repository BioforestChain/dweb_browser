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
    @EnvironmentObject var toolbarState: ToolBarState
    @EnvironmentObject var selectedTab: SelectedTab
    @EnvironmentObject var addressBarState: AddressBarState
    @EnvironmentObject var openingLink: OpeningLink
    @EnvironmentObject var addressBar: AddressBarState
    @EnvironmentObject var webcacheStore: WebCacheStore
    @EnvironmentObject var dragScale: WndDragScale

    @State private var toolbarHeight: CGFloat = toolBarH
//    @State private var showMoreSheet = false
    @State private var cancellables: Set<AnyCancellable> = []
    
    private var isShowingWebsite: Bool { webcacheStore.cache(at: selectedTab.curIndex).shouldShowWeb }
    
    var body: some View {
        GeometryReader { geo in
            ZStack {
                if toolbarState.shouldExpand {
                    fiveButtons
                } else {
                    threeButtons
                }
            }.frame(height: geo.size.height)
        }
//        .onAppear {
//            selectedTab.$curIndex
//                .sink { newIndex in
//                    Log("Value changed: \(newIndex)")
//                    tabIndexChanged(to: newIndex)
//                }
//                .store(in: &cancellables) // Store the subscription
//        }
//        .onDisappear {
//            cancellables.forEach { $0.cancel() }
//            cancellables.removeAll()
//        }
    }

    var threeButtons: some View {
        ZStack {
            GeometryReader { geo in
                let size = geo.size
                HStack(alignment: .center) {
                    Spacer().frame(width: size.width / 15)

                    BiColorButton(imageName: "add", disabled: false) {
                        Log("open new tab was clicked")
                        toolbarState.createTabTapped = true
                    }
                    .frame(height: min(size.width / 14, size.height / 1.9))

                    Spacer()
                    Text("\(webcacheStore.cacheCount)个标签页")
                        .foregroundColor(Color.ToolbarColor)
                        .font(dragScale.scaledFont())
                        .fontWeight(.semibold)

                    Spacer()

                    Button {
                        toolbarState.shouldExpand = true
                    } label: {
                        Text("完成")
                            .foregroundColor(Color.dwebTint)
                            .font(dragScale.scaledFont())
                            .fontWeight(.semibold)
                    }

                    Spacer().frame(width: size.width / 15)
                }
                .frame(height: size.height)
            }
        }
    }

    var fiveButtons: some View {
        ZStack {
            HStack(alignment: .center) {
                Spacer()

                Button(action: {
                    toolbarState.creatingDesktopLink.toggle()
                    print("trying to appnd an link on desktop")
                }) {
                    Image(systemName: "apps.iphone.badge.plus")
                        .renderingMode(.template)
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                        .background(Color.bkColor)
                        .foregroundColor(isShowingWebsite ? Color.ToolbarColor : Color.gray)
                        .frame(minWidth: toolItemMinWidth, maxWidth: toolItemMaxWidth, minHeight: toolItemMinWidth, maxHeight: toolItemMaxWidth)
                }
                .disabled(!isShowingWebsite)
               
                Spacer()
                if isShowingWebsite {
                    BiColorButton(imageName: "add", disabled: false) {
                        Log("open new tab was clicked")
                        toolbarState.createTabTapped = true
                    }
                } else {
                    BiColorButton(imageName: "scan", disabled: false) {
                        Log("scan qrcode")
                        toolbarState.isPresentingScanner = true
                    }
                }
                Spacer()
                Group {
                    BiColorButton(imageName: "shift", disabled: false) {
                        Log("shift tab was clicked")
                        toolbarState.shouldExpand = false
                    }
                    Spacer()
                    BiColorButton(imageName: "more", disabled: false) {
                        withAnimation {
                            toolbarState.showMoreMenu = true
//                            showMoreSheet = true
                        }
                        Log("more menu was clicked")
                    }
                    Spacer()
                }

                .onReceive(addressBarState.$isFocused) { isFocused in
                    withAnimation {
                        toolbarHeight = isFocused ? 0 : toolBarH
                    }
                }
                .clipped()
                .sheet(isPresented: $toolbarState.isPresentingScanner) {
                    CodeScannerView(codeTypes: [.qr], showViewfinder: true) { response in
                        if case let .success(result) = response {
                            // TODO: 扫描结果result.string
                            Log(result.string)
                            let url = URL(string: result.string)
                            if url?.scheme == "dweb" {
                                DwebDeepLink.shared.openDeepLink(url: result.string)
                            } else {
                                toolbarState.isPresentingScanner = false
                                addressBar.inputText = result.string
                                let url = URL.createUrl(addressBar.inputText)
                                DispatchQueue.main.async {
                                    openingLink.clickedLink = url
                                    addressBar.isFocused = false
                                }
                            }
                        }
                    }
                }
            }
        }
    }

//    func tabIndexChanged(to index: Int) {
//        let currentWrapper = webcacheStore.webWrapper(at: index)
//        toolbarState.canGoBack = currentWrapper.canGoBack
//        toolbarState.canGoForward = currentWrapper.canGoForward
//    }
}

let toolItemMinWidth = 14.0
let toolItemMaxWidth = toolItemMinWidth * 2
let toolBarMinHeight = toolItemMinWidth + 4.0

let addressbarMinHeight = toolBarMinHeight
