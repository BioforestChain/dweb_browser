//
//  ToolBarView.swift
//  TableviewDemo
//
//  Created by ui06 on 4/16/23.
//

import SwiftUI

class ToolbarState: ObservableObject {
    @Published var canGoForward = false
    @Published var canGoBack = false
    @Published var addTapped = false
    @Published var moreTapped = false
    @Published var showOptions = false  //显示多标签页
    
    @Published var newPageTapped = false
    @Published var doneTapped = false
}

struct ToolbarView: View {
    
    @EnvironmentObject var states: ToolbarState
    
    var body: some View {
        if states.showOptions{
            HStack(spacing: 5){
                
                Group{
                    Spacer()
                        .frame(width: 25)
                    ToolbarItem(imageName: "chevron.backward") {
                        states.canGoBack = true
                        print("arrow.up was clicked")
                    }
                    
                    Spacer()
                    
                    ToolbarItem(imageName: "chevron.forward") {
                        states.canGoForward.toggle()
                        print("arrow.up was clicked")
                    }
                    
                    Spacer()
                    
                    ToolbarItem(imageName: "plus") {
                        states.addTapped.toggle()
                        
                        print("arrow.up was clicked")
                    }
                }
                Group{
                    Spacer()
                    
                    ToolbarItem(imageName: "book") {
                        print("arrow.up was clicked")
                        states.moreTapped = true
                    }.sheet(isPresented: $states.moreTapped) {
                        states.moreTapped = false
                    } content: {
                        HistoryView()
                    }
                    
                    Spacer()
                    
                    ToolbarItem(imageName: "doc.on.doc") {
                        states.showOptions.toggle()
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
                    states.newPageTapped.toggle()
                }
                
                Spacer()
                Text("15个标签页")
                Spacer()
                
                ToolbarItem(title: "完成") {
                    states.doneTapped.toggle()
                    states.showOptions.toggle()
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
