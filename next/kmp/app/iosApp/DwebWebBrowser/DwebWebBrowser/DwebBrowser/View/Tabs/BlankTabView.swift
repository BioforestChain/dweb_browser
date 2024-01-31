//
//  HomePageView.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/6/6.
//

import SwiftUI
struct BlankTabView: View {
    @EnvironmentObject var dragScale: WndDragScale

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
                        .font(.system(size: 23 * width / screen_width))
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
