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
    
    @EnvironmentObject var showSheet: ShowSheet
    @ObservedObject var wrapperMgr = WebWrapperMgr.shared
    
    private let itemSize = CGSize(width: 28, height: 28)
    @State private var toolbarHeight: CGFloat = toolBarH
//    @State private var : Bool = true
    
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
            }
            
            Spacer()
            Text("\(wrapperMgr.store.count)个标签页")
                .font(.system(size: 18, weight: .medium))
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
                    }
                } else {
                    BiColorButton(size: itemSize, imageName: "add", disabled: false) {
                        print("open new tab was clicked")
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
                        showSheet.should = true
                    }
                    print("more menu was clicked")
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
                    .foregroundColor(.dwebTint)
            }
        }
    }
}
