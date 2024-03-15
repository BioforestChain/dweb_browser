//
//  BookmarkView.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/4/22.
//

import SwiftData
import SwiftUI

struct BookmarkView: View {
    @Environment(WndDragScale.self) var dragScale
    @Environment(OpeningLink.self) var openingLink
    @Environment(ToolBarState.self) var toolBarState

    @State private var bookmarks: [BrowserWebSiteInfo] = browserViewDataSource.loadBookmarksToBrowser() ?? []

    var body: some View {

        ZStack {
            Color.clear
                .accessibilityElement()// UITest占位用，不要删掉。
            if bookmarks.count > 0 {
                List {
                    ForEach(bookmarks, id: \.self) { bookmark in
                        HStack {
                            Image(uiImage: bookmark.icon)
                                .resizable()
                                .frame(width: dragScale.properValue(max: 24),
                                       height: dragScale.properValue(max: 24))
                                .cornerRadius(4)
                            Text(bookmark.data.title)
                                .font(dragScale.scaledFont_16)
                                .lineLimit(1)
                                .padding(.leading, 6)
                            Spacer()
                        }
                        .onTapGesture {
                            guard let bookmarkUrl = URL(string: bookmark.data.url) else { return }
                            openingLink.clickedLink = bookmarkUrl
                            toolBarState.showMoreMenu = false
                        }
                    }
                    .onDelete(perform: deleteBookmarkData)
                }
                .accessibilityElement(children: .contain)
                .accessibilityIdentifier("BookmarkView_List")
            } else {
                NoResultView(empty: .bookmark)
                    .accessibilityElement(children: .contain)
                    .accessibilityIdentifier("BookmarkView_Empty")
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityIdentifier("BookmarkView")
    }

    func deleteBookmarkData(_ indexSet: IndexSet) {
        for index in indexSet {
            let element = bookmarks.remove(at: index)
            browserViewDataSource.removeBookmark(bookmark: element.id)
        }
    }
}

#Preview {
    BookmarkView()
        .modelContainer(for: Bookmark.self)
}
