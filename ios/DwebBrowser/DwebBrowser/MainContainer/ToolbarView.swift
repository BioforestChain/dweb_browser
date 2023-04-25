//
//  ToolBarView.swift
//  TableviewDemo
//
//  Created by ui06 on 4/16/23.
//

import SwiftUI


struct ToolbarView: View {
    @State var height: CGFloat = 50
    @State var showHistoryView: Bool = false

    var body: some View {
        HStack(spacing: 5){
            
            Group{
                Spacer()
                
                ToolbarItem(imageName: "chevron.backward") {
                    print("arrow.up was clicked")
                }
                
                Spacer()
                
                ToolbarItem(imageName: "chevron.forward") {
                    print("arrow.up was clicked")
                }
                
                Spacer()
                
                ToolbarItem(imageName: "square.and.arrow.up") {
                    print("arrow.up was clicked")
                }
            }
            Group{
                Spacer()
                
                ToolbarItem(imageName: "pencil") {
                    print("arrow.up was clicked")
                }
                
                Spacer()
                
                ToolbarItem(imageName: "book") {
                    print("arrow.up was clicked")
                    showHistoryView = true
                }.sheet(isPresented: $showHistoryView) {
                    showHistoryView = false
                } content: {
                    HistoryView()
                }
                
                Spacer()
                
                ToolbarItem(imageName: "doc.on.doc") {
                    print("arrow.up was clicked")
                }
                
                Spacer()
            }
        }
        .frame(width: screen_width,height: height)
        .background(Color.white)
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
