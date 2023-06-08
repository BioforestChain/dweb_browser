//
//  SheetHistoryView.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/5/5.
//

import SwiftUI

struct SheetHistoryView: View {
    
    @ObservedObject var viewModel: HistoryViewModel
    @State private var searchText = ""
    @State var type: NoResultEnum
    
    var body: some View {
        
        if viewModel.sections.count > 0 {
            Form {
                ForEach(0..<viewModel.sections.count, id: \.self) { i in
                    let section = viewModel.sections[i]
                    Section {
                        ForEach(0..<section.items.count, id: \.self) { j in
                            let item = section.items[j]
                            if i == viewModel.sections.count - 1, j == section.items.count - 1 {
                                SheetHistoryCell(linkRecord: item, isLast: true, isLoadmore: true, viewModel: viewModel)
                                    .listRowInsets(EdgeInsets(top: 0, leading: 0, bottom: 0, trailing: 0))
                            } else {
                                SheetHistoryCell(linkRecord: item, isLast: j == section.items.count - 1 ? true : false, isLoadmore: false, viewModel: viewModel)
                                    .listRowInsets(EdgeInsets())
                                    
                            }
                        }
                        .onDelete { indexSet in
                            deleteHistoryData(at: section.items, offsets: indexSet)
                        }
                    } header: {
                        HStack {
                            Text(Date.historyTime(timeString: section.id))
                                .frame(height: 30)
                            Spacer()
                        }
                    }
                    .textCase(nil)
                    .listRowInsets(EdgeInsets())
                    .listRowSeparator(.hidden)
                }
            }
        } else {
            NoResultView(imageName: HistoryConfig(type).noResultImageName, title: HistoryConfig(type).noResultTitle)
        }
        /*
        VStack {
            
//            SearchBarView(text: $searchText, placeholder: HistoryConfig(type).searchTitle)
//                .onChange(of: searchText) { newValue in
//                    viewModel.searchHistory(for: newValue)
//                }
            
            if viewModel.sections.count > 0 {
                List {
                    ForEach(0..<viewModel.sections.count, id: \.self) { i in
                        let section = viewModel.sections[i]
                        Section(header:
                                    Text(Date.historyTime(timeString: section.id))
                            .frame(height: 30)
                            .background(.red)
                        ) {
                            ForEach(0..<section.items.count, id: \.self) { j in
                                let item = section.items[j]
                                if i == viewModel.sections.count - 1, j == section.items.count - 1 {
                                    SheetHistoryCell(linkRecord: item, isLast: true, viewModel: viewModel)
                                        .listRowInsets(EdgeInsets())
                                } else {
                                    SheetHistoryCell(linkRecord: item, isLast: false, viewModel: viewModel)
                                        .listRowInsets(EdgeInsets())
                                }
                            }
                            .onDelete { indexSet in
                                deleteHistoryData(at: section.items, offsets: indexSet)
                            }
                        }
                        .listRowInsets(EdgeInsets(top: 0, leading: 16, bottom: 0, trailing: 16))
                    }
                    
                }
                .listStyle(.grouped)
                .background(SwiftUI.Color.init(red: 245.0/255, green: 246.0/255, blue: 247.0/255, opacity: 1))
                .navigationTitle(HistoryConfig(type).title)
                .navigationBarTitleDisplayMode(.inline)
            } else {
                NoResultView(imageName: HistoryConfig(type).noResultImageName, title: HistoryConfig(type).noResultTitle)
            }
        }
        .background(Color(hexString: "F5F6F7"))*/
    }
    
    @ViewBuilder
    func bookmarkListView() -> some View {
        
        
    }
    
    private func deleteHistoryData(at items: [LinkRecord], offsets: IndexSet) {
        
        offsets.forEach { index in
            if index < items.count {
                let model = items[index]
                viewModel.deleteSingleHistory(for: model.id.uuidString)
            }
        }
    }
}

//struct SheetHistoryView_Previews: PreviewProvider {
//    static var previews: some View {
//        SheetHistoryView()
//    }
//}
