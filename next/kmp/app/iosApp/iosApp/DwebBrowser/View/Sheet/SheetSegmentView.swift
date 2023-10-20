//
//  HalfSheetPickerView.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/5/5.
//

import SwiftUI

enum SheetCategory: String{
    case menu = "menu_set"
    case bookmark = "bookmark"
    case history = "history"
}

struct SheetSegmentView: View {
    @EnvironmentObject var selectedTab:SelectedTab
    @State var selectedCategory = SheetCategory.bookmark
    var isShowingWeb: Bool
    var categoryList: [SheetCategory] {
        isShowingWeb ? [.menu, .bookmark, .history] : [.bookmark, .history]
    }
    
    var body: some View {
        VStack{
            Picker("Select image", selection: $selectedCategory) {
                ForEach(categoryList, id: \.self) {
                    Image(uiImage: .assetsImage(name: ($0.rawValue)))
                }
            }
            .pickerStyle(.segmented)
            .padding(.horizontal,16)
            
            if selectedCategory == .menu {
                MenuView()
                    .padding(.vertical, 16)
                Spacer()
            } else if selectedCategory == .bookmark {
                BookmarkView()
            } else if selectedCategory == .history {
                HistoryView(histories: HistoryMgr())
            }
        }
        .padding(.top, 28)
        .background(Color.bkColor)
    }
}

struct HalfSheetPickerView_Previews: PreviewProvider {
    static var previews: some View {
        SheetSegmentView(selectedCategory: .bookmark, isShowingWeb: false)
    }
}
