//
//  BookmarkCell.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/5/29.
//

import SwiftUI

struct BookmarkCell: View {
    @Environment(\.presentationMode) var presentationMode
    @EnvironmentObject var openingLink: OpeningLink
    @EnvironmentObject var selectedTab: SelectedTab
    @EnvironmentObject var dragScale: WndDragScale
    @EnvironmentObject var toolBarState: ToolBarState


    var linkRecord: LinkRecord
    var isLast: Bool
    var loadMoreAction: ()->Void
    private var dividerWidth: CGFloat { isLast ? 0 : 1000.0 }
    var iconUrl: URL { URL(string: linkRecord.websiteIcon) ?? .defaultWebIconURL }
    var body: some View {
        ZStack(alignment: .leading){

            VStack{
                HStack(spacing: dragScale.properValue(floor: 6, ceiling: 12)) {
                    WebsiteIconImage(iconUrl: iconUrl)
                        .frame(width: dragScale.properValue(floor: 16, ceiling: 28), height: dragScale.properValue(floor: 16, ceiling: 28))
                        .cornerRadius(4)
                        .padding(.leading, 12)
                    
                    Text(linkRecord.title)
                        .font(.system(size: dragScale.scaledFontSize(maxSize: 16)))
                        .foregroundColor(Color.menuTitleColor)
                    Spacer()
                }
            }
            .frame(height: dragScale.properValue(floor: 32, ceiling: 50))
            .background(Color.menubkColor)
            
            .overlay(
                Divider().frame(width: dividerWidth, height: 0.5), // 添加分割线视图
                alignment: .bottom // 对齐到底部
            )
            .onAppear {
                guard isLast else { return }
                loadMoreAction()
            }
            
            Color(white: 0.01, opacity: 0.01) // 添加一个空的透明视图
                 .onTapGesture {
                     guard let link = URL(string: linkRecord.link) else { return }
                     openingLink.clickedLink = link
                     toolBarState.showMoreMenu = false
//                     presentationMode.wrappedValue.dismiss()
                 }
        }
    }
}

