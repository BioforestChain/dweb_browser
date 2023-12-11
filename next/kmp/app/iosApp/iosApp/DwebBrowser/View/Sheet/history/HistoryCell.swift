//
//  HistoryCell.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/5/5.
//

import SwiftUI
import DwebShared
struct HistoryCell: View {
    @Environment(\.presentationMode) var presentationMode
    @EnvironmentObject var openingLink: OpeningLink
    @EnvironmentObject var dragScale: WndDragScale
    @EnvironmentObject var toolBarState: ToolBarState
    
    var linkRecord: BrowserWebSiteInfo
    var isLast: Bool
//    @Binding var shouldShowWeb: Bool
    var loadMoreAction: ()->Void
    private var dividerWidth: CGFloat { isLast ? 0 : 1000.0 }
    var body: some View {
        ZStack(alignment: .leading){

            VStack(alignment: .leading, spacing: 5){
                Text(linkRecord.title)
                    .font(.system(size: dragScale.scaledFontSize(maxSize: 16)))
                    .foregroundColor(Color.menuTitleColor)
                
                Text(linkRecord.url)
                    .font(.system(size: dragScale.scaledFontSize(maxSize: 11)))
                    .foregroundColor(Color(hexString: "ACB5BF"))
                
            }
            .padding(.horizontal,16)
            .frame(height: dragScale.properValue(floor: 32, ceiling: 60))
            
            
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
                     guard let link = URL(string: linkRecord.url) else { return }
                     openingLink.clickedLink = link
                     toolBarState.showMoreMenu = false
//                     presentationMode.wrappedValue.dismiss()
                 }
        }
        .background(Color.menubkColor)
    }
}

