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
    var deleteAction: (WebCache) -> Void

    var body: some View {
        Self._printChanges()

        return ZStack(alignment: .topTrailing) {
            VStack(spacing: 0) {
                Image(uiImage: .snapshotImage(from: webCache.snapshotUrl))
                    .resizable()
                    .aspectRatio(contentMode: .fill)
                    .frame(width: gridCellW, height: cellImageH, alignment: .top)
                    .cornerRadius(gridcellCornerR)
                    .clipped()
                    .overlay(RoundedRectangle(cornerRadius: 10)
                        .stroke(Color.dwebTint, lineWidth: 2)
                        .opacity(isSelected ? 1 : 0)
                    )
                HStack {
                    WebsiteIconImage(iconUrl: webCache.webIconUrl)
                        .frame(width: 22, height: 22)

                    Text(webCache.title)
                        .fontWeight(.semibold)
                        .lineLimit(1)
                }.frame(height: gridcellBottomH)
            }
            deleteButton
        }
    }

    var deleteButton: some View {
        Button {
            deleteAction(webCache)
        } label: {
            Image("tab_close")
        }
        .padding(.top, 8)
        .padding(.trailing, 8)
    }
}

struct GridCell_Previews: PreviewProvider {
    static var previews: some View {
        //        GridCell(webCache: WebCache.example,isSelected: )
        //            .frame(width: 200,height: 300)
        Text("")
    }
}
