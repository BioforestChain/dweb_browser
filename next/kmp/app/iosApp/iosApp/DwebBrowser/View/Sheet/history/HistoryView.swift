//
//  SheetHistoryView.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/5/5.
//

import SwiftUI

struct HistoryView: View {
    @State private var searchText = ""
    @StateObject var histories = HistoryMgr()
    @EnvironmentObject var selectedTab: SelectedTab
    @EnvironmentObject var dragScale: WndDragScale
    var body: some View {
        
        if histories.sections.count <= 0 {
            NoResultView(config: .history)
        } else {
            Form {
                ForEach(0..<histories.sections.count, id: \.self) { section in
                    let section = histories.sections[section]
                    Section {
                        ForEach(0..<section.items.count, id: \.self) { row in
                            let item = section.items[row]
                            let isLast = histories.sections.last?.items.last == item
                            HistoryCell(linkRecord: item, isLast: isLast){
                                    //click load more
                                    histories.loadMoreHistoryData()
                                }
                                .listRowInsets(isLast ? EdgeInsets(.zero) : EdgeInsets())
                        }
                        .onDelete { indexSet in
                            deleteHistoryData(at: section.items, offsets: indexSet)
                        }
                    } header: {
                        HStack {
                            Text(Date.historyTime(timeString: section.id))
                                .font(dragScale.scaledFont())
                                .frame(height: dragScale.properValue(floor: 15, ceiling: 30))
                            Spacer()
                        }
                    }
                    .textCase(nil)
                    .listRowInsets(EdgeInsets())
                    .listRowSeparator(.hidden)
                }
            }
        }
    }
    
    @ViewBuilder
    func bookmarkListView() -> some View {
        
        
    }
    
    private func deleteHistoryData(at items: [LinkRecord], offsets: IndexSet) {
        
        offsets.forEach { index in
            if index < items.count {
                let model = items[index]
                histories.deleteHistory(for: model.id.uuidString)
            }
        }
    }
}

//struct SheetHistoryView_Previews: PreviewProvider {
//    static var previews: some View {
//        SheetHistoryView()
//    }
//}
