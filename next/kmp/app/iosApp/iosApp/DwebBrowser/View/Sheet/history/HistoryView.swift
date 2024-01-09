//
//  SheetHistoryView.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/5/5.
//

import DwebShared
import SwiftUI

struct HistoryView: View {
    @EnvironmentObject var dragScale: WndDragScale
    @StateObject var historyStore = DwebBrowserHistoryStore.shared

    var body: some View {
        VStack {
            if historyStore.sections.count <= 0 {
                NoResultView(config: .history)
            } else {
                List {
                    ForEach(historyStore.sections.indices, id: \.self) { (index: Int) in
                        let section = historyStore.sections[index]
                        Section(section.id) {
                            ForEach(section.items.indices, id: \.self) { (index: Int) in
                                let record = section.items[index]
                                HStack {
                                    VStack(alignment: .leading, spacing: 5) {
                                        Text(record.title != "" ? record.title : " ")
                                            .foregroundStyle(Color.primary)
                                            .font(dragScale.scaledFont(maxSize: 16))

                                        Text(record.url)
                                            .foregroundStyle(Color(.systemGray3))
                                            .font(dragScale.scaledFont(maxSize: 8))
                                    }
                                    .lineLimit(1)

                                    Spacer()
                                }
                                .padding(.top, 4)
                                .onAppear {
                                    if index == section.items.count - 1 {
                                        // TODO: 加载更多
                                        print("asking for more")
//                                        historyStore.loadNextHistorys()
                                    }
                                }
                            }
                            .onDelete { indexSet in
                                historyStore.removeHistoryRecord(at: section, indexSet: indexSet)
                            }
                        }
                    }
                }
                .task {
                    historyStore.loadHistory()
                }
            }
        }
    }
}

#Preview(body: {
    HistoryView()
})
