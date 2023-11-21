//
//  BookmarkView.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/4/22.
//

import SwiftUI
import SwiftData

struct BookmarkView: View {
    @StateObject var viewModel = BookmarkViewModel()
    
    var body: some View {
        if viewModel.dataSources.count > 0 {
            VStack {
                RoundedRectangle(cornerRadius: 10)
                    .fill(Color.white)
                    .shadow(radius: 2)
                    .overlay(
                        List {
                            ForEach(viewModel.dataSources) {  link in
                                BookmarkCell(linkRecord: link, isLast: link.id == viewModel.dataSources.last?.id, loadMoreAction: {viewModel.loadMoreHistoryData()})
                            }
                            .onDelete { indexSet in
                                deleteBookmarkData(at: viewModel.dataSources, offsets: indexSet)
                            }
                            .textCase(nil)
                            .listRowInsets(EdgeInsets())
                            .listRowSeparator(.hidden)
                        }
                    )
            }
        } else {
            NoResultView(config: .bookmark)
        }
    }
    
    private func deleteBookmarkData(at items: [LinkRecord], offsets: IndexSet) {
        
        offsets.forEach { index in
            if index < items.count {
                let model = items[index]
                viewModel.deleteSingleHistory(for: model.id.uuidString)
            }
        }
    }
}



struct BookmarkView2: View {
    @EnvironmentObject var dragScale: WndDragScale
    @Environment(\.modelContext) var modelContext
    @Query var bookmarks: [Bookmark]
    
    var body: some View {
        ZStack{
            List {
                ForEach(bookmarks) {  bookmark in
                    HStack(alignment: .center){
                        WebsiteIconImage(iconUrl: URL(string: bookmark.iconUrl)!)
                            .frame(width: dragScale.properValue(floor: 16, ceiling: 28), height: dragScale.properValue(floor: 16, ceiling: 28))
                            .cornerRadius(4)
                            .padding(.leading, 12)
                        Text(bookmark.title)

                    }
                }
                .onDelete(perform: deleteBookmarkData)
                .textCase(nil)
                .listRowInsets(EdgeInsets())
                .listRowSeparator(.hidden)
            }
            
            Button("add bookmark", action: addBookMark)
        }
        
    }
    
    func deleteBookmarkData(indexSet: IndexSet){
        
    }
    
    func addBookMark(){
        let baidu = Bookmark(link: "https://www.baidu.com", iconUrl: URL.defaultSnapshotURL.absoluteString)
        let apple = Bookmark(link: "https://www.apple.com", iconUrl: URL.defaultSnapshotURL.absoluteString)
        modelContext.insert(baidu)
        modelContext.insert(apple)
    }
}

#Preview {
    BookmarkView2()
}
