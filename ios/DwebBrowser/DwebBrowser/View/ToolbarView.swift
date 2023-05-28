//
//  ToolBarView.swift
//  TableviewDemo
//
//  Created by ui06 on 4/16/23.
//

import SwiftUI
import UIKit

struct ToolbarView: View {
    @EnvironmentObject var tabstate: TabState
    @State var moreTapped = false
    @ObservedObject var wrapperMgr = WebWrapperMgr.shared
    var body: some View {
        if !tabstate.showTabGrid{
            HStack(spacing: 5){
                Group{
                    Spacer()
                        .frame(width: 25)
                    ToolbarItem(imageName: "chevron.backward") {
//                        toolbarStates.canGoBack = true
                        print("backward was clicked")
                    }
                    
                    Spacer()
                    
                    ToolbarItem(imageName: "chevron.forward") {
//                        toolbarStates.canGoForward.toggle()
                        print("forwardp was clicked")
                    }
                    
                    Spacer()
                    
                    ToolbarItem(imageName: "plus") {
//                        toolbarStates.addTapped.toggle()
                        print("plus was clicked")
                    }
                }
                Group{
                    Spacer()
                    
                    ToolbarItem(imageName: "book") {
                        print("book was clicked")
                        withAnimation {
                            moreTapped = true
                            
                        }
                    }.sheet(isPresented: $moreTapped) {
                        withAnimation {
                            moreTapped = false
                        }
                    } content: {
                        HistoryView()
                    }
                    
                    Spacer()
                    
                    ToolbarItem(imageName: "doc.on.doc") {
                        tabstate.showTabGrid = true
                        print("arrow.up was clicked")
                    }
                    
                    Spacer()
                        .frame(width: 25)
                }
            }
            .frame(height: toolBarHeight)
            
        }else{
            HStack(spacing: 5){
                Spacer()
                    .frame(width: 25)
                
                ToolbarItem(imageName: "plus") {
//                    toolbarStates.newPageTapped.toggle()
                }
                
                Spacer()
                Text("\(wrapperMgr.wrapperStore.count)个标签页")
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
        ToolbarView()
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
            }
        }
    }
}
