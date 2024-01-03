//
//  SheetHistoryView.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/5/5.
//

import SwiftUI
// mike todo: import DwebShared

struct HistoryView: View {
    @EnvironmentObject var dragScale: WndDragScale
    @StateObject var historyStore = DwebBrowserHistoryStore.shared
    
    var body: some View {
        
        VStack {
            if historyStore.sections.count <= 0 {
                NoResultView(config: .history)
            } else {
                Form {
                    ForEach(0..<historyStore.sections.count, id: \.self) { section in
                        let section = historyStore.sections[section]
                        Section {
                            ForEach(section.items) { item in
                                let isLast = historyStore.sections.last?.items.last == item
                                HistoryCell(linkRecord: item, isLast: isLast){
//                                        DwebBrowserHistoryStore.shared.loadNextHistorys()
                                    }
                                    .listRowInsets(isLast ? EdgeInsets(.zero) : EdgeInsets())
                            }
                            .onDelete { indexSet in
                                historyStore.removeHistoryRecord(at: section, indexSet: indexSet)
                            }
                        } header: {
                            HStack {
                                Text(section.id)
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
        }.task {
            historyStore.loadHistory()
        }
    }
}


