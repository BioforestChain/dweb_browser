//
//  MenuView.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/5/6.
//

import SwiftUI
struct MenuView: View {
    @EnvironmentObject var dragScale: WndDragScale
    @State private var viewmodel = MenuViewModel()
    
    let webCache: WebCache
    var body: some View {
        ZStack {
            ScrollView(.vertical) {
                VStack(spacing: dragScale.properValue(floor: 5, ceiling: 16)) {
                    Button {
                        viewmodel.addToBookmark(cache: webCache)
                    } label: {
                        MenuCell(title: "添加书签", imageName: "bookmark")
                    }

                    ShareLink(item: webCache.lastVisitedUrl.absoluteString) {
                        MenuCell(title: "分享", imageName: "share")
                    }

                    tracelessView
                }
                .padding(.vertical, dragScale.properValue(floor: 10, ceiling: 32))
                .background(.bk)
                .frame(maxWidth: .infinity)
            }
        }
    }

    var tracelessView: some View {
        HStack {
            Text("无痕模式")
                .foregroundColor(.primary)
                .font(.system(size: dragScale.scaledFontSize(maxSize: 16)))
                .padding(.leading, 16)

            Toggle("", isOn: $viewmodel.isTraceless)
                .scaleEffect(dragScale.onWidth)
        }
        .frame(height: 50 * dragScale.onWidth)
        .background(Color.menubk)
        .cornerRadius(6)
        .padding(.horizontal, 16)
        .onChange(of: viewmodel.isTraceless) { _, newValue in
            TracelessMode.shared.isON = newValue
        }
    }
}
