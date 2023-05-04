//
//  ToolBarView.swift
//  TableviewDemo
//
//  Created by ui06 on 4/16/23.
//

import SwiftUI

class ToolbarState: ObservableObject {
    @Published var button1Clicked = false
    @Published var button2Clicked = false
    @Published var button3Clicked = false
    @Published var button4Clicked = false
    @Published var button5Clicked = false
    @Published var showMenu = true
    
}

struct ToolbarView: View {
    
    @EnvironmentObject var states: ToolbarState

    var body: some View {
        HStack(spacing: 5){
            
            Group{
                Spacer()
                
                ToolbarItem(imageName: "chevron.backward") {
                    states.button1Clicked = true
                    print("arrow.up was clicked")
                }
                
                Spacer()
                
                ToolbarItem(imageName: "chevron.forward") {
                    states.button2Clicked.toggle()
                    print("arrow.up was clicked")
                }
                
                Spacer()
                
                ToolbarItem(imageName: "square.and.arrow.up") {
                    states.button3Clicked.toggle()

                    print("arrow.up was clicked")
                }
            }
            Group{
                Spacer()
                
                ToolbarItem(imageName: "pencil") {
                    states.button4Clicked.toggle()

                    print("arrow.up was clicked")
                }
                
                Spacer()
                
                ToolbarItem(imageName: "book") {
                    print("arrow.up was clicked")
                    states.button5Clicked = true
                }.sheet(isPresented: $states.button5Clicked) {
                    states.button5Clicked = false
                } content: {
                    HistoryView()
                }
                
                Spacer()
                
                ToolbarItem(imageName: "doc.on.doc") {
                    states.showMenu.toggle()
                    print("arrow.up was clicked")
                }
                
                Spacer()
            }
        }
        .frame(height: toolBarHeight)
    }
}

struct ToolbarView_Previews: PreviewProvider {
    static var previews: some View {
        ToolbarView()
    }
}

struct ToolbarItem: View {
    var imageName: String
    var tapAction: ()->()
    var body: some View {
        
        Button(action: {
            tapAction()
        }) {
            Image(systemName: imageName)
                .resizable()
                .scaledToFit()
                .frame(height: 25)
        }
    }
}
