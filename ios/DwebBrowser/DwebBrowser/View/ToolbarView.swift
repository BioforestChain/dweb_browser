//
//  ToolBarView.swift
//  TableviewDemo
//
//  Created by ui06 on 4/16/23.
//

import SwiftUI
import UIKit

struct ToolbarView: View {
    @EnvironmentObject var tabstate: BottomViewState
    @EnvironmentObject var selectedTab: SelectedTab

    @State var moreTapped = false
    @ObservedObject var wrapperMgr = WebWrapperMgr.shared
    
    private let itemSize = CGSize(width: 28, height: 28)

    var body: some View {
        if !tabstate.showTabGrid{
            HStack(spacing: 5){
                Group{
                    Spacer()
                        .frame(width: 25)
                    BiColorButton(size: itemSize, imageName: "back", disabled: !tabstate.canGoBack) {
                        tabstate.goBackTapped = true
                    }
                    
                    Spacer()
                    BiColorButton(size: itemSize, imageName: "forward", disabled: !tabstate.canGoForward) {
                        tabstate.goForwardTapped = true
                    }
                    Spacer()
                    BiColorButton(size: itemSize, imageName: "add", disabled: false) {
                        print("open new tab was clicked")
                    }
                    
                }
                Group{
                    Spacer()
                    BiColorButton(size: itemSize, imageName: "shift", disabled: false) {
                        print("shift tab was clicked")
                        tabstate.showTabGrid = true
                    }
                    Spacer()
                    
                    BiColorButton(size: itemSize, imageName: "more", disabled: false) {
                        withAnimation {
                            moreTapped = true
                        }
                        print("more menu was clicked")
                    }.sheet(isPresented: $moreTapped) {
                        withAnimation {
                            moreTapped = false
                        }
                    } content: {
                        HistoryView()
                    }
                    
                    Spacer()
                        .frame(width: 25)
                }
            }
            .frame(height: toolBarHeight)
            .onChange(of: selectedTab.curIndex, perform: { index in
                let currentWrapper = wrapperMgr.store[index]
                tabstate.canGoBack = currentWrapper.canGoBack
                tabstate.canGoForward = currentWrapper.canGoForward
            })
            
        }else{
            HStack(spacing: 5){
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
                    tabstate.showTabGrid = false
                }
                .fontWeight(.semibold)
                Spacer()
                    .frame(width: 25)
            }
            .frame(height: toolBarHeight)
        }
    }
}

struct ToolbarView_Previews: PreviewProvider {
    static var previews: some View {
//        ToolbarView()
        Text("aa")
    }
}

struct ToolbarItem: View {
    var imageName: String?
    var title: String?
    var tapAction: ()->()
    var body: some View {
        
        Button(action: {
            tapAction()
        }) {
            if imageName != nil{
                Image(systemName: imageName!)
                    .resizable()
                    .scaledToFit()
                    .frame(height: 25)
            }else{
                Text(title!)
                    .foregroundColor(.dwebTint)
            }
        }
    }
}
