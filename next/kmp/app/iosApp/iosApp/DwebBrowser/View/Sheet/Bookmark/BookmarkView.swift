//
//  BookmarkView.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/4/22.
//

import SwiftData
import SwiftUI

struct BookmarkView: View {
    @EnvironmentObject var dragScale: WndDragScale
    @Environment(\.modelContext) var modelContext
    @Query var bookmarks: [Bookmark]

    var body: some View {
        ZStack {
            Form {
                ForEach(bookmarks, id: \.self) { bookmark in
                    HStack {
                        WebsiteIconImage(iconUrl: URL(string: bookmark.iconUrl)!)
                            .frame(width: dragScale.properValue(floor: 14, ceiling: 24),
                                   height: dragScale.properValue(floor: 14, ceiling: 24))
                            .cornerRadius(4)

                        Text(bookmark.title)
                            .font(.system(size: dragScale.scaledFontSize(maxSize: 16)))
                            .lineLimit(1)
                            .padding(.leading, 6)
                    }
                    .frame(height: 30)
                }
                .onDelete(perform: deleteBookmarkData)
            }
        }
    }

    func deleteBookmarkData(_ indexSet: IndexSet) {
        for index in indexSet {
            let bookmark = bookmarks[index]
            modelContext.delete(bookmark)
        }
    }
}

#Preview {
    BookmarkView()
        .modelContainer(for: Bookmark.self)
}
