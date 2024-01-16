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
    @ObservedObject var webCache: WebCache
    var isSelected: Bool
    @EnvironmentObject var selectedTab: SelectedTab
    @EnvironmentObject var deleteCache: DeleteCache
    @EnvironmentObject var dragScale: WndDragScale

    var body: some View {
        ZStack(alignment: .topTrailing) {
            GeometryReader { geo in

                VStack(spacing: 0) {
                    if webCache.shouldShowWeb {
                        Image(uiImage: webCache.snapshotImage)
                            .resizable()
                            .aspectRatio(contentMode: .fill)
                            .frame(width: geo.size.width, height: geo.size.height * cellImageHeightRatio)
                            .cornerRadius(gridcellCornerR)
                            .overlay(RoundedRectangle(cornerRadius: 10)
                                .stroke(Color.cellBorder, lineWidth: 2)
                                .opacity(isSelected ? 1 : 0)
                            )
                            .clipped()
                    } else {
                        BlankTabView(scale: geo.size.width / screen_width * 1.2)
                            .frame(width: geo.size.width, height: geo.size.height * cellImageHeightRatio)
                            .background(Color.bkColor)
                            .cornerRadius(gridcellCornerR)
                            .overlay(RoundedRectangle(cornerRadius: 10)
                                .stroke(isSelected ? Color.cellBorderColor : Color.bkColor, lineWidth: 2)
                            )
                            .clipped()
                    }
                    HStack {
                        WebsiteIconImage(iconUrl: webCache.webIconUrl)
                            .aspectRatio(contentMode: .fit)
                            .frame(height: geo.size.height * 0.1)
                        
                        Text(webCache.title)
                            .font(.system(size: dragScale.scaledFontSize(maxSize: 20))) // 设置字体大小为 20，粗细为 semibold
                            .lineLimit(1)
                    }
                    .padding(.vertical, 3)
                    .frame(height: geo.size.height * cellTitleHeightRatio)
                }
                .contentShape(Rectangle())
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

struct ImagePartialView: View {

  var image: Image

  var body: some View {

    GeometryReader { geo in

      image
        .resizable()
        .frame(width: 800, height: 1000)
        .clipped()
        .offset(x: 100, y: 200)
        .frame(width: 300, height: 450)

    }

  }
}

struct GridCell_Previews: PreviewProvider {
    static var previews: some View {
        ImagePartialView(image: Image(uiImage: UIImage(named: "dweb_icon")! ))
            .frame(width: 300,height: 600)
            .background(.green)
    }
}
