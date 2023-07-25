//
//  BookmarkView.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/4/22.
//

import SwiftUI

struct BookmarkView: View {
    @StateObject var viewModel = BookmarkViewModel()
    
    var body: some View {
        if viewModel.dataSources.count > 0 {
            VStack {
                
                RoundedRectangle(cornerRadius: 10)
                    .fill(Color.white)
                    .shadow(radius: 2)
                    .padding()
                    .overlay(
                        List {
                            ForEach(viewModel.dataSources) {  link in
                                BookmarkCell(linkRecord: link, isLast: link.id == viewModel.dataSources.last?.id, loadMoreAction: {viewModel.loadMoreHistoryData()})
                                    .frame(height: 50)
                            }
                            .onDelete { indexSet in
                                deleteBookmarkData(at: viewModel.dataSources, offsets: indexSet)
                            }
                            .textCase(nil)
                            .listRowInsets(EdgeInsets())
                            .listRowSeparator(.hidden)
                        }
                    )
                
                Spacer()
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
