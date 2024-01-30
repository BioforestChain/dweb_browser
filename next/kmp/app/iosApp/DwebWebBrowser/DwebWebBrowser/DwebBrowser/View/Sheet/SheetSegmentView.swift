//
//  HalfSheetPickerView.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/5/5.
//

import SwiftUI

enum SheetCategory: String {
    case menu = "menu_set"
    case bookmark
    case history
}

struct SheetSegmentView: View {
    let webCache: WebCache
    
    @EnvironmentObject var dragScale: WndDragScale
    @State private var selectedCategory = SheetCategory.bookmark
    
    var categoryList: [SheetCategory] {
        webCache.shouldShowWeb ? [.menu, .bookmark, .history] : [.bookmark, .history]
    }
    var body: some View {
        VStack {
            HStack {
                RoundedRectangle(cornerRadius: 10)
                    .foregroundColor(Color.secondary)
                    .frame(width: dragScale.properValue(floor: 45, ceiling: 60), height: dragScale.properValue(floor: 6, ceiling: 9))
            }
            .frame(height: dragScale.properValue(floor: 20, ceiling: 30))
            
            Picker("Select image", selection: $selectedCategory) {
                ForEach(categoryList, id: \.self) {
                    Image(uiImage: .assetsImage(name: $0.rawValue))
                        .resizable()
                        .frame(height: dragScale.properValue(floor: 13, ceiling: 26))

                }
            }
            .pickerStyle(.segmented)
            .padding(.horizontal, 16)
            .frame(height: dragScale.properValue(floor: 20, ceiling: 30))


            if selectedCategory == .menu {
                MenuView(webCache: webCache)
            } else if selectedCategory == .bookmark {
                BookmarkView()
            } else if selectedCategory == .history {
                HistoryView()
            }

            Spacer()
        }
        .background(Color.bk)
        .cornerRadius(gridcellCornerR)
        .modelContainer(for: Bookmark.self)
    }
}


