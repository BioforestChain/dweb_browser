//
//  HomePageView.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/6/6.
//

import SwiftUI
struct BlankTabView: View {
    @EnvironmentObject var dragScale: WndDragScale
    private let pageWidth: CGFloat = 60
    private let pageHeight: CGFloat = 210
    private let fontSize: CGFloat = 23
    
    var scale: CGFloat?
    
    var body: some View {
        VStack {
            VStack {
                Image(.dwebIcon)
                    .resizable()
                    .frame(width: dragScale.properValue(floor: scale == nil ? pageWidth : pageWidth * scale!, ceiling: scale == nil ? pageHeight : pageHeight * scale!), height: dragScale.properValue(floor: scale == nil ? pageWidth : pageWidth * scale!, ceiling: scale == nil ? pageHeight : pageHeight * scale!))
                Text("Dweb Browser")
                    .font(dragScale.scaledFont(maxSize: scale == nil ? fontSize : fontSize * scale!))
            }
        }
    }
}

struct HomePageView_Previews: PreviewProvider {
    static var previews: some View {
        BlankTabView()
    }
}
