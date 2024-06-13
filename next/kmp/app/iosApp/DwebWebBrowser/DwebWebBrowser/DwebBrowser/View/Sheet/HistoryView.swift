//
//  SheetHistoryView.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/5/5.
//

import SwiftUI

struct HistoryView: View {
    @Environment(WndDragScale.self) var dragScale
    @Environment(OpeningLink.self) var openingLink
    @Environment(ToolBarState.self) var toolBarState

    @StateObject var historyStore = DwebBrowserHistoryStore.shared

    var body: some View {
        VStack {
            if historyStore.sections.count <= 0 {
                NoResultView(empty: .history)
                    .accessibilityElement(children: .contain)
                    .accessibilityIdentifier("HistoryView_Empty")
            } else {
                List {
                    ForEach(historyStore.sections.indices, id: \.self) { (index: Int) in
                        let section = historyStore.sections[index]
                        Section(section.id) {
                            ForEach(section.items.indices, id: \.self) { (index: Int) in
                                let record = section.items[index]
                                HStack {
                                    VStack(alignment: .leading, spacing: 5) {
                                        Text(record.data.title != "" ? record.data.title : " ")
                                            .foregroundStyle(Color.primary)
                                            .font(dragScale.scaledFont_16)

                                        Text(record.data.url)
                                            .foregroundStyle(Color(.systemGray3))
                                            .font(dragScale.scaledFont_8)
                                    }
                                    .lineLimit(1)

                                    Spacer()
                                }
                                .onTapGesture {
                                    guard let historyUrl = URL(string: record.data.url) else { return }
                                    openingLink.clickedLink = historyUrl
                                    toolBarState.showMoreMenu = false
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
                .accessibilityElement(children: .contain)
                .accessibilityIdentifier("HistoryView_List")
                
            }
        }
        .background(content: {
            Color.clear
                .accessibilityElement()// UITest占位用，不要删掉。
        })
        .accessibilityElement(children: .contain)
        .accessibilityIdentifier("HistoryView")
        .task {
            historyStore.loadHistory()
        }
    }
}

#Preview(body: {
    HistoryView()
})
