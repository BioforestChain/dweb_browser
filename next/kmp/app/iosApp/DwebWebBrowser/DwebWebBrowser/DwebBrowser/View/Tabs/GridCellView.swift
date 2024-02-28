//
//  GridCell.swift
//  DwebBrowser
//
//  Created by ui06 on 5/29/23.
//

import SwiftUI

struct WebsiteIconImage: View {
    var iconUrl: URL
    var body: some View {
        ZStack {
            if iconUrl.isFileURL {
                Image(uiImage: .defaultWebIconImage)
                    .resizable()
                    .foregroundColor(.primary)
            } else {
                AsyncImage(url: iconUrl) { phase in
                    if let image = phase.image {
                        image.resizable()
                            .aspectRatio(contentMode: .fit)
                    } else {
                        Image(uiImage: .defaultWebIconImage)
                            .resizable()
                    }
                }
            }
        }
    }
}

struct GridCell: View {
    var webCache: WebCache
    var isSelected: Bool
    @Environment(DeleteCache.self) var deleteCache
    @Environment(WndDragScale.self) var dragScale

    var body: some View {
        ZStack(alignment: .topTrailing) {
            GeometryReader { geo in
                VStack(spacing: 0) {
                    ZStack {
                        let shape = RoundedRectangle(cornerRadius: gridcellCornerR)
                        Group {
                            if webCache.isWebVisible {
                                Image(uiImage: webCache.snapshotImage)
                                    .resizable()
                                    .aspectRatio(contentMode: .fill)
                                    .frame(width: geo.size.width)
                            } else {
                                BlankTabView()
                            }
                        }
                        .frame(height: geo.size.height * cellImageHeightRatio)
                        .background(Color.bk)
                        .cornerRadius(gridcellCornerR)

                        shape.strokeBorder(lineWidth: 3)
                            .foregroundColor(.cellBorder)
                            .opacity(isSelected ? 1 : 0)
                    }

                    HStack {
                        WebsiteIconImage(iconUrl: webCache.webIconUrl)
                            .aspectRatio(contentMode: .fit)
                            .frame(height: geo.size.height * 0.1)

                        Text(webCache.title)
                            .font(dragScale.scaledFont_20) // 设置字体大小为 20，粗细为 semibold
                            .lineLimit(1)
                    }
                    .frame(height: geo.size.height * 0.15)

//                    .padding(.vertical, 3)
                }
            }
            deleteButton
        }
    }

    var deleteButton: some View {
        Button {
            deleteCache.cacheId = webCache.id
        } label: {
            Image(systemName: "xmark.circle.fill")
                .resizable()
                .frame(width: dragScale.onWidth * 28, height: dragScale.onWidth * 28)
                .foregroundColor(.gray)
                .background(Color.white)
                .cornerRadius(100)
        }
        .padding(.top, 8)
        .padding(.trailing, 8)
    }
}
