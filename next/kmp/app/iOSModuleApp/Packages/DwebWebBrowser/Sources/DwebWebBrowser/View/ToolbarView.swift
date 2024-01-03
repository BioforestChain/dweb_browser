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
    @EnvironmentObject var openingLink: OpeningLink
    @EnvironmentObject var addressBar: AddressBarState
    @EnvironmentObject var dragScale: WndDragScale

    @State private var toolbarHeight: CGFloat = toolBarH
    @State private var loadingDone: Bool = false

    let webCount: Int
    let isWebVisible: Bool
    @ObservedObject var webMonitor: WebWrapper

    private var canCreateDesktopLink: Bool { isWebVisible && loadingDone }

    var body: some View {
        GeometryReader { _ in
            if toolbarState.shouldExpand {
                fiveButtons
            } else {
                threeButtons
            }
        }
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
                    Text("\(webCount)个标签页")
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
                        .foregroundColor(canCreateDesktopLink ? Color.ToolbarColor : Color.gray)
                        .frame(minWidth: toolItemMinWidth, maxWidth: toolItemMaxWidth, minHeight: toolItemMinWidth, maxHeight: toolItemMaxWidth)
                }
                .onChange(of: webMonitor.webMonitor.isLoadingDone) { oldValue, newValue in
                    if oldValue != newValue {
                        loadingDone = newValue
                    }
                }
                .disabled(!canCreateDesktopLink)

                Spacer()
                if isWebVisible {
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
                        }
                        Log("more menu was clicked")
                    }
                    Spacer()
                }

                .clipped()
                .sheet(isPresented: $toolbarState.isPresentingScanner) {
                    CodeScannerView(codeTypes: [.qr], showViewfinder: true) { response in
                        toolbarState.isPresentingScanner = false
                        if case let .success(result) = response {
                            Log(result.string)
                            let url = URL(string: result.string)
                            if url?.scheme == "dweb" {
                                DwebDeepLink.shared.openDeepLink(url: result.string)
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
                }
            }
        }
    }
}

internal let toolItemMinWidth = 14.0
internal let toolItemMaxWidth = toolItemMinWidth * 2
internal let toolBarMinHeight = toolItemMinWidth + 4.0

internal let addressbarMinHeight = toolBarMinHeight
