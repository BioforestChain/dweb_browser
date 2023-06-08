//
//  BookmarkView.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/4/22.
//

import SwiftUI

struct BookmarkView: View {
    
    @ObservedObject var viewModel: BookmarkViewModel
    
    var body: some View {
        
        if viewModel.dataSources.count > 0 {
            VStack {
                Form {
                    Section {
                        ForEach(viewModel.dataSources) {  item in
                            BookmarkCell(linkRecord: item, isLast: false, isLoadmore: false, viewModel: viewModel)
                                .frame(height: 50)
                        }
                    }
                    .textCase(nil)
                    .listRowInsets(EdgeInsets())
                    .listRowSeparator(.hidden)
                }
                .listStyle(.grouped)
                .navigationViewStyle(.stack)
                .navigationBarHidden(true)

                Spacer()
            }
        } else {
            NoResultView(imageName: HistoryConfig(.bookmark).noResultImageName, title: HistoryConfig(.bookmark).noResultTitle)
        }
    }
}


