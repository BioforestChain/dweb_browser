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
    @EnvironmentObject var selectedTab: SelectedTab
    @EnvironmentObject var dragScale: WndDragScale

    @ObservedObject var localLinkSearcher = LocalLinkSearcher.shared
    @State private var tapHasBeenHandled = false
    var body: some View {
        Form {
            Section {
                ForEach(0 ..< webSearchers.count, id: \.self) { index in
                    let searcher = webSearchers[index]
                    HStack(spacing: dragScale.properValue(floor: 6, ceiling: 12)) {
                        Image(uiImage: .assetsImage(name: searcher.icon))
                            .resizable()
                            .frame(width: dragScale.properValue(floor: 12, ceiling: 30), height: dragScale.properValue(floor: 12, ceiling: 30))
                            .padding(.leading, dragScale.properValue(floor: 8, ceiling: 16))

                        VStack(alignment: .leading, spacing: 4, content: {
                            Text(searcher.name)
                                .foregroundColor(Color.menuTitleColor)
                                .font(.system(size: dragScale.scaledFontSize(maxSize: 18)))
                                .lineLimit(1)

                            Text(paramURLAbsoluteString(with: addressBar.inputText))
                                .foregroundColor(Color(hexString: "ACB5BF"))
                                .font(.system(size: dragScale.scaledFontSize(maxSize: 12)))
                                .lineLimit(1)
                                .padding(.trailing, 8)
                        })
                        Spacer()
                    }
                    .background(Color.menubkColor)
                    .onTapGesture {
                        guard let url = URL(string: searcher.inputHandler(addressBar.inputText)) else { return }
                        UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
                        openingLink.clickedLink = url
                        addressBar.isFocused = false
                        tapHasBeenHandled = true
                    }
                    .frame(height: dragScale.properValue(floor: 36, ceiling: 50))
                }
            } header: {
                Text("搜索引擎")
                    .foregroundColor(Color.menuTitleColor)
                    .font(.system(size: dragScale.scaledFontSize(maxSize: 18)))
                    .padding(.top, 10)
                    .padding(.bottom, 6)

            }
            .textCase(nil)
            .listRowInsets(EdgeInsets())
        }
        .scrollContentBackground(.hidden)
        .background(Color.bkColor)
        .onTapGesture {
            if !tapHasBeenHandled {
                releaseFocuse()
            }
        }
    }

    func releaseFocuse() {
        UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
        addressBar.inputText = ""
        addressBar.isFocused = false
    }
}
