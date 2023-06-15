//
//  HalfSheetPickerView.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/5/5.
//

import SwiftUI

enum SheetCategory: String{
    case menu = "ico_menu_set"
    case bookmark = "ico_menu_bookmark"
    case history = "ico_menu_history"
}

struct SheetSegmentView: View {
    @State var selectedCategory = SheetCategory.bookmark

    @EnvironmentObject var selectedTab:SelectedTab
    @ObservedObject var webcacheMgr = WebCacheMgr.shared
    var categoryList: [SheetCategory] {
        let showWeb = webcacheMgr.store[selectedTab.curIndex].lastVisitedUrl != testURL
        return showWeb ? [.menu, .bookmark, .history] : [.bookmark, .history]
    }
    
    var body: some View {
        VStack{
            Picker("Select image", selection: $selectedCategory) {
                ForEach(categoryList, id: \.self) {
                    Image($0.rawValue)
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
                HistoryView()
            }
        }
        .background(Color.bkColor)
    }
}

struct HalfSheetPickerView_Previews: PreviewProvider {
    static var previews: some View {
        SheetSegmentView()
    }
}
