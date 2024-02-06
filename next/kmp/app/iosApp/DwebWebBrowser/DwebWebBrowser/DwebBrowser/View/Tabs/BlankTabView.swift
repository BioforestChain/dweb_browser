//
//  HomePageView.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/6/6.
//

import SwiftUI
struct BlankTabView: View {
    @Environment(WndDragScale.self) var dragScale

    var body: some View {
        GeometryReader { geo in
            let width = geo.size.width
            ZStack {
                Color.bk
                VStack {
                    Image(.dwebIcon)
                        .resizable()
                        .frame(width: width * 0.5, height: width * 0.5)
                    Text("Dweb Browser")
                        .font(dragScale.scaledFont_22)
                }
            }
        }
    }
}

struct HomePageView_Previews: PreviewProvider {
    static var previews: some View {
        BlankTabView()
    }
}
