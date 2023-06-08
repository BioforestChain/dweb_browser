//
//  HistoryListView.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/4/19.
//

import SwiftUI

struct HistoryListView: View {
    
    @ObservedObject var viewModel: HistoryViewModel
    @State private var searchText: String = ""
   
    @State var historyOperateList: [String] = []
    
    @State var type: NoResultEnum
    
    var body: some View {
        
        VStack {
            
            NavigationView {
                
                if viewModel.sections.count > 0 {
                    List {
                        ForEach(viewModel.sections) { section in
                            
                            Section(header:
                                        Text("\(section.id)")
                                .frame(height: 30)
                            ) {
                                ForEach(section.items) {  item in
                                    LinkHistoryCell(linkRecord: item, deleteList: $historyOperateList)
                                        .listRowSeparator(.hidden)
                                        .listRowInsets(EdgeInsets())
                                        .listRowBackground(SwiftUI.Color.init(red: 245.0/255, green: 246.0/255, blue: 247.0/255, opacity: 1))
                                    
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
            .searchable(text: $searchText, prompt: HistoryConfig(type).searchTitle)
            .onChange(of: searchText) { newValue in
                viewModel.searchHistory(for: newValue)
            }
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button {
                        searchText = ""
                    } label: {
                        Image("xmark.circle.fill")
                            .foregroundColor(.gray)
                    }

                }
            }
            
            Button("删除") {
                deleteHistoryData()
            }
            .frame(maxWidth: .infinity, maxHeight: 40)
            .background(.white)
            .foregroundColor(historyOperateList.count > 0 ? .red : .gray)
            .disabled(historyOperateList.isEmpty)
            
            Spacer()
        }
        .background(SwiftUI.Color.init(red: 245.0/255, green: 246.0/255, blue: 247.0/255, opacity: 1))
        .onAppear {
           
        }
    }
    
    //delete history data
    private func deleteHistoryData() {
        guard historyOperateList.count > 0 else { return }
        viewModel.deleteHistory(for: historyOperateList)
    }
}

struct HistoryListView_Previews: PreviewProvider {
    static var previews: some View {
        HistoryListView(viewModel: LinkHistoryViewModel(), type: .linkHistory)
    }
}

