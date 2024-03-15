//
//  ShowSearchHolderView.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/6/6.
//

import SwiftUI

struct SearchResultView: View {
    @Environment(AddressBarState.self) var addressBar
    @Environment(OpeningLink.self) var openingLink
    @Environment(WndDragScale.self) var dragScale
    @Environment(WebCacheStore.self) var cacheStore
    @Environment(SelectedTab.self) var seletedTab

    @State private var tapHasBeenHandled = false
    
    private var curCache: WebCache { cacheStore.cache(at: seletedTab.index)}
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
                                .accessibilityIdentifier(searcher.name)
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
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .simultaneousGesture(TapGesture().onEnded { _ in
                        tapHasBeenHandled = true
                        addressBar.isFocused = false
                        guard let url = URL(string: searcher.inputHandler(addressBar.inputText.trim)) else { return }
                        openingLink.clickedLink = url
                    })
                }

            } header: {
                Text("搜索引擎")
                    .foregroundColor(.primary)
                    .font(dragScale.scaledFont_16)
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
            }
        }
        .scrollContentBackground(.hidden)
        .background(.bk)
        .accessibilityElement(children: .combine)
        .accessibilityIdentifier("SearchResultView")
    }
}

