//
//  ShowSearchHolderView.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/6/6.
//

import SwiftUI

struct SearchResultView: View {
    @EnvironmentObject var addressBar: AddressBarState
    @EnvironmentObject var openingLink: OpeningLink
    @Environment(WndDragScale.self) var dragScale

    @State private var tapHasBeenHandled = false
    var body: some View {
        Form {
            Section {
                ForEach(0 ..< webSearchers.count, id: \.self) { index in
                    let searcher = webSearchers[index]

                    HStack(spacing: dragScale.properValue(max: 12)) {
                        Image(uiImage: .assetsImage(name: searcher.icon))
                            .resizable()
                            .frame(width: dragScale.properValue(max: 30), height: dragScale.properValue(max: 30))
                            .padding(.leading, dragScale.properValue(max: 16))

                        VStack(alignment: .leading, spacing: 4) {
                            Text(searcher.name)
                                .foregroundColor(.primary)
                                .font(dragScale.scaledFont_18)
                                .lineLimit(1)
                            Text(addressBar.inputText.trim)
                                .foregroundColor(Color(.systemGray2))
                                .font(dragScale.scaledFont_12)
                                .lineLimit(1)
                                .padding(.trailing, 8)
                        }
                        Spacer()
                    }

                    .frame(height: dragScale.properValue(max: 50))
                    .contentShape(Rectangle())

                    .highPriorityGesture(TapGesture().onEnded { _ in
                        tapHasBeenHandled = true
                        addressBar.isFocused = false
                        guard let url = URL(string: searcher.inputHandler(addressBar.inputText)) else { return }
                        openingLink.clickedLink = url
                    })
                }

            } header: {
                Text("搜索引擎")
                    .foregroundColor(.primary)
                    .font(dragScale.scaledFont_12)
                    .padding(.top, 10)
                    .padding(.bottom, 6)
            }
            .textCase(nil)
            .listRowInsets(EdgeInsets())
        }
        .onAppear {
            tapHasBeenHandled = false
        }
        .onTapGesture {
            if !tapHasBeenHandled {
                addressBar.isFocused = false
                addressBar.inputText = ""
            }
        }
        .scrollContentBackground(.hidden)
        .background(.bk)
    }
}
