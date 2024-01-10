//
//  BookmarkView.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/4/22.
//

import SwiftData
import SwiftUI
import DwebShared

struct BookmarkView: View {
    @EnvironmentObject var dragScale: WndDragScale
    @EnvironmentObject var openingLink: OpeningLink
    @EnvironmentObject var toolBarState: ToolBarState

    @State private var bookmarks: [BrowserWebSiteInfo] = (browserService.loadBookmarks() as? [BrowserWebSiteInfo]) ?? []
    
    var body: some View {

        ZStack {
            if bookmarks.count > 0 {
                List {
                    ForEach(bookmarks, id: \.self) { bookmark in
                        HStack {
                            let image = browserService.webSiteInfoIconToUIImage(web: bookmark) ?? UIImage(systemName: "book")!
                            Image(uiImage: image)
                                .resizable()
                                .frame(width: dragScale.properValue(floor: 14, ceiling: 24),
                                       height: dragScale.properValue(floor: 14, ceiling: 24))
                                .cornerRadius(4)

                            Text(bookmark.title)
                                .font(.system(size: dragScale.scaledFontSize(maxSize: 16)))
                                .lineLimit(1)
                                .padding(.leading, 6)
                            Spacer()
                        }
                        .onTapGesture {
                            guard let bookmarkUrl = URL(string: bookmark.url) else { return }
                            openingLink.clickedLink = bookmarkUrl
                            toolBarState.showMoreMenu = false
                        }
                    }
                    .onDelete(perform: deleteBookmarkData)
                }
            } else {
                NoResultView(empty: .bookmark)
            }
        }
    }

    func deleteBookmarkData(_ indexSet: IndexSet) {
        for index in indexSet {
            let element = bookmarks.remove(at: index)
            browserService.removeBookmark(bookmark: element.id)
        }
    }
}

#Preview {
    BookmarkView()
        .modelContainer(for: Bookmark.self)
}
