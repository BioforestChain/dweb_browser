//
//  ToolBarView.swift
//  TableviewDemo
//
//  Created by ui06 on 4/16/23.
//

import SwiftUI
import UIKit

struct ToolbarView: View {
    @EnvironmentObject var toolbarState: ToolBarState
    @EnvironmentObject var selectedTab: SelectedTab
    @EnvironmentObject var addressBarState: AddressBarState
    @EnvironmentObject var openingLink: OpeningLink
    @EnvironmentObject var addressBar: AddressBarState

    @ObservedObject var wrapperMgr = WebWrapperMgr.shared
    private let itemSize = CGSize(width: 28, height: 28)
    @State private var toolbarHeight: CGFloat = toolBarH
    
    @State private var isPresentingScanner = false
    @State private var showMoreSheet = false

    var body: some View {
        if toolbarState.shouldExpand {
            fiveButtons
        } else {
            threeButtons
        }
    }
    
    var threeButtons: some View {
        HStack(spacing: 5) {
            Spacer()
                .frame(width: 25)
            
            BiColorButton(size: itemSize, imageName: "add", disabled: false) {
                print("open new tab was clicked")
                toolbarState.createTabTapped = true

            }
            
            Spacer()
            Text("\(wrapperMgr.store.count)个标签页")
                .font(.system(size: 18, weight: .medium))
                .foregroundColor(Color.ToolbarColor)
            Spacer()
            
            ToolbarItem(title: "完成") {
                toolbarState.shouldExpand = true
            }
            .fontWeight(.semibold)
            Spacer()
                .frame(width: 25)
        }
        .frame(height: toolbarHeight)
        .background(Color.bkColor)
    }
    
    var fiveButtons: some View {
        HStack(spacing: 5) {
            Group {
                Spacer()
                    .frame(width: 25)
                BiColorButton(size: itemSize, imageName: "back", disabled: !toolbarState.canGoBack) {
                    toolbarState.goBackTapped = true
                }
                
                Spacer()
                BiColorButton(size: itemSize, imageName: "forward", disabled: !toolbarState.canGoForward) {
                    toolbarState.goForwardTapped = true
                }
                Spacer()
                if WebCacheMgr.cache(at: selectedTab.curIndex).shouldShowWeb {
                    BiColorButton(size: itemSize, imageName: "scan", disabled: false) {
                        print("scan qrcode")
                        isPresentingScanner = true
                    }
                } else {
                    BiColorButton(size: itemSize, imageName: "add", disabled: false) {
                        print("open new tab was clicked")
                        toolbarState.createTabTapped = true
                    }
                }
            }
            Group {
                Spacer()
                BiColorButton(size: itemSize, imageName: "shift", disabled: false) {
                    print("shift tab was clicked")
                    toolbarState.shouldExpand = false
                }
                Spacer()
                
                BiColorButton(size: itemSize, imageName: "more", disabled: false) {
                    withAnimation {
                        showMoreSheet = true
                    }
                    printWithDate(msg: "more menu was clicked")
                }
                
                Spacer()
                    .frame(width: 25)
            }
        }
        .frame(height: toolbarHeight)
        .background(Color.bkColor)

        .onChange(of: selectedTab.curIndex, perform: { index in
            let currentWrapper = wrapperMgr.store[index]
            toolbarState.canGoBack = currentWrapper.canGoBack
            toolbarState.canGoForward = currentWrapper.canGoForward
        })
        .onReceive(addressBarState.$isFocused) { isFocused in
            withAnimation {
                toolbarHeight = isFocused ? 0 : toolBarH
            }
        }
        .clipped()
        .sheet(isPresented: $isPresentingScanner) {
            CodeScannerView(codeTypes: [.qr], showViewfinder: true) { response in
                if case let .success(result) = response {
                    //TODO  扫描结果result.string
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
        
        .sheet(isPresented: $showMoreSheet){
            SheetSegmentView(selectedCategory: WebCacheMgr.shared.store[selectedTab.curIndex].shouldShowWeb ? .menu : .bookmark)
                .environmentObject(selectedTab)
                .environmentObject(openingLink)
                .presentationDetents([.medium, .large])
        }
    }
}

struct ToolbarItem: View {
    var imageName: String?
    var title: String?
    var tapAction: () -> ()
    var body: some View {
        Button(action: {
            tapAction()
        }) {
            if imageName != nil {
                Image(systemName: imageName!)
                    .resizable()
                    .scaledToFit()
                    .frame(height: 25)
            } else {
                Text(title!)
                    .foregroundColor(Color.dwebTint)
            }
        }
    }
}
