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

    @State private var toolbarHeight: CGFloat = toolBarH
    @State private var isPresentingScanner = false
    @State private var showMoreSheet = false
    @State private var cancellables: Set<AnyCancellable> = []

    @State private var wndWidth: CGFloat = .zero

    var body: some View {
        ZStack {
            if toolbarState.shouldExpand {
                fiveButtons
            } else {
                threeButtons
            }
        }
        .background(.yellow)

        .onAppear {
            selectedTab.$curIndex
                .sink { newIndex in
                    print("Value changed: \(newIndex)")
                    tabIndexChanged(to: newIndex)
                }
                .store(in: &cancellables) // Store the subscription
        }
        .onDisappear {
            cancellables.forEach { $0.cancel() }
            cancellables.removeAll()
        }
    }

    var threeButtons: some View {
        GeometryReader{ geo in
        HStack(alignment: .center) {
            Spacer()
//                .frame(width: 25)

            BiColorButton(imageName: "add", disabled: false) {
                print("open new tab was clicked")
                toolbarState.createTabTapped = true
            }

            Spacer()
            Text("\(webcacheStore.cacheCount)个标签页")
//                .font(.system(size: 18, weight: .medium))
                .foregroundColor(Color.ToolbarColor)
                .font(.system(size: 10))
            Spacer()

            Button {
                toolbarState.shouldExpand = true

            } label: {
                Text("完成")
                    .foregroundColor(Color.dwebTint)
                    .font(.system(size: 10))

//                    .fontWeight(.semibold)
            }

            Spacer()
//                .frame(width: 25)
        }
        }
        .frame(height: toolbarHeight)
        .background(Color.bkColor)
    }

    var fiveButtons: some View {
        ZStack {
            GeometryReader { geo in
                HStack(alignment: .center) {
                    Spacer()
                    BiColorButton(imageName: "back", disabled: !toolbarState.canGoBack) {
                        toolbarState.goBackTapped = true
                    }
                    Spacer()
                    BiColorButton(imageName: "forward", disabled: !toolbarState.canGoForward) {
                        toolbarState.goForwardTapped = true
                    }
                    Spacer()
                    if webcacheStore.cache(at: selectedTab.curIndex).shouldShowWeb {
                        BiColorButton(imageName: "scan", disabled: false) {
                            print("scan qrcode")
                            isPresentingScanner = true
                        }
                    } else {
                        BiColorButton(imageName: "add", disabled: false) {
                            print("open new tab was clicked")
                            toolbarState.createTabTapped = true
                        }
                    }
                    Spacer()
                    Group {
                        BiColorButton(imageName: "shift", disabled: false) {
                            print("shift tab was clicked")
                            toolbarState.shouldExpand = false
                        }
                        Spacer()
                        BiColorButton(imageName: "more", disabled: false) {
                            withAnimation {
                                showMoreSheet = true
                            }
                            printWithDate("more menu was clicked")
                        }
                        Spacer()
                    }
                }

                .frame(height: geo.size.height)
                .background(Color.bkColor)
                .onReceive(addressBarState.$isFocused) { isFocused in
                    withAnimation {
                        toolbarHeight = isFocused ? 0 : toolBarH
                    }
                }
                .clipped()
                .sheet(isPresented: $isPresentingScanner) {
                    CodeScannerView(codeTypes: [.qr], showViewfinder: true) { response in
                        if case let .success(result) = response {
                            // TODO: 扫描结果result.string
                            print(result.string)
                            isPresentingScanner = false
                            addressBar.inputText = result.string
                            let url = URL.createUrl(addressBar.inputText)
                            DispatchQueue.main.async {
                                openingLink.clickedLink = url
                                addressBar.isFocused = false
                            }
                        }
                    }
                }

                .sheet(isPresented: $showMoreSheet) {
                    SheetSegmentView(isShowingWeb: webcacheStore.cache(at: selectedTab.curIndex).shouldShowWeb)
                        .environmentObject(selectedTab)
                        .environmentObject(openingLink)
                        .presentationDetents([.medium, .large])
                }
            }
        }
    }

    func tabIndexChanged(to index: Int) {
        let currentWrapper = webcacheStore.webWrapper(at: index)
        toolbarState.canGoBack = currentWrapper.canGoBack
        toolbarState.canGoForward = currentWrapper.canGoForward
    }
}
