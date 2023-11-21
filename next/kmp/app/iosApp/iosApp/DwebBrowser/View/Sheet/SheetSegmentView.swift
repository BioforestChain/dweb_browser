//
//  HalfSheetPickerView.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/5/5.
//

import SwiftUI
import SwiftData
enum SheetCategory: String {
    case menu = "menu_set"
    case bookmark
    case history
}

struct SheetSegmentView: View {
    @EnvironmentObject var selectedTab: SelectedTab
    @EnvironmentObject var dragScale: WndDragScale
    @State var selectedCategory = SheetCategory.bookmark
    var isShowingWeb: Bool
    var categoryList: [SheetCategory] {
        isShowingWeb ? [.menu, .bookmark, .history] : [.bookmark, .history]
    }

    var body: some View {
        VStack {
            HStack {
                RoundedRectangle(cornerRadius: 10)
                    .foregroundColor(Color.sheetTopbar)
                    .frame(width: dragScale.properValue(floor: 45, ceiling: 60), height: dragScale.properValue(floor: 6, ceiling: 9))
            }
            .frame(height: dragScale.properValue(floor: 20, ceiling: 30))
            
            Picker("Select image", selection: $selectedCategory) {
                ForEach(categoryList, id: \.self) {
                    Image(uiImage: .assetsImage(name: $0.rawValue))
                }
            }
            .pickerStyle(.segmented)
            .padding(.horizontal, 16)

            if selectedCategory == .menu {
                MenuView()
            } else if selectedCategory == .bookmark {
                BookmarkView2()
                    .modelContainer(for: Bookmark.self)

            } else if selectedCategory == .history {
                HistoryView()
            }

            Spacer()
        }
        .background(Color.bkColor)
        .cornerRadius(gridcellCornerR)
    }
}

struct HalfSheetPickerView_Previews: PreviewProvider {
    static var previews: some View {
        SheetSegmentView(selectedCategory: .bookmark, isShowingWeb: false)
    }
}
