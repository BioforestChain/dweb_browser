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
    
    @Environment(WndDragScale.self) var dragScale
    @State private var selectedCategory = SheetCategory.bookmark
    
    var categoryList: [SheetCategory] {
        webCache.isWebVisible ? [.menu, .bookmark, .history] : [.bookmark, .history]
    }
    var body: some View {
        VStack {
            HStack {
                RoundedRectangle(cornerRadius: 10)
                    .foregroundColor(Color.secondary)
                    .frame(width: dragScale.properValue(max: 60), height: dragScale.properValue(max: 9))
            }
            .frame(height: dragScale.properValue(max: 30))
            
            Picker("Select image", selection: $selectedCategory) {
                ForEach(categoryList, id: \.self) {
                    Image(uiImage: .assetsImage(name: $0.rawValue))
                        .resizable()
                        .frame(height: dragScale.properValue(max: 26))

                }
            }
            .pickerStyle(.segmented)
            .accessibilityElement()
            .accessibilityIdentifier("morePicker")
            .padding(.horizontal, 16)
            .frame(height: dragScale.properValue(max: 30))


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
        .accessibilityElement(children: .contain)
        .accessibilityIdentifier("SheetSegmentView")
    }
}


