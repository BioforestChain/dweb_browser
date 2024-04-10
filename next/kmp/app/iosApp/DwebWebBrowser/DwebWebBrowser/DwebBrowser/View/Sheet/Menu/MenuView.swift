//
//  MenuView.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/5/6.
//

import SwiftUI
struct MenuView: View {
    @Environment(WndDragScale.self) var dragScale
    @Environment(ResizeSheetState.self) var resizeSheetState
    @State private var viewmodel = MenuViewModel()
    @State private var showDownloadView = false
    let webCache: WebCache
    var body: some View {
        ZStack {
            ScrollView(.vertical) {
                VStack(spacing: dragScale.properValue(max: 16)) {
                    Button {
                        viewmodel.addToBookmark(cache: webCache)
                    } label: {
                        MenuCell(title: "添加书签", imageName: "bookmark")
                    }

                    ShareLink(item: webCache.lastVisitedUrl.absoluteString) {
                        MenuCell(title: "分享", imageName: "share")
                    }

                    tracelessView
                    
                    Button {
                        resizeSheetState.presenting = false
                        showDownloadView = true
                    } label: {
                        downloadView
                            .navigationDestination(isPresented: $showDownloadView) {
                                DownloadListView()
                            }
                    }
                }
                .padding(.vertical, dragScale.properValue(max: 32))
                .background(.bk)
                .frame(maxWidth: .infinity)
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityIdentifier("MenuView")
    }

    var tracelessView: some View {
        HStack {
            Text("无痕模式")
                .foregroundColor(.primary)
                .font(dragScale.scaledFont_16)
                .padding(.leading, 16)

            Toggle("", isOn: $viewmodel.isTraceless)
                .scaleEffect(dragScale.onWidth)
                .accessibilityIdentifier("MenuView_TackToggle")
        }
        .frame(height: 50 * dragScale.onWidth)
        .background(Color.menubk)
        .cornerRadius(6)
        .padding(.horizontal, 16)
        .onChange(of: viewmodel.isTraceless) { _, newValue in
            TracelessMode.shared.isON = newValue
        }
    }
    
    var downloadView: some View {
        HStack {
            Text("下载")
                .foregroundColor(.primary)
                .font(dragScale.scaledFont_16)
                .padding(.leading, 16)

            Spacer()
            
            Image(systemName: "arrowshape.down")
                .font(.system(size: 22))
                .padding(12)
                .scaleEffect(dragScale.onWidth)
        }
        .tint(.black)
        .frame(height: 50 * dragScale.onWidth)
        .background(Color.menubk)
        .cornerRadius(6)
        .padding(.horizontal, 16)
    }
}
