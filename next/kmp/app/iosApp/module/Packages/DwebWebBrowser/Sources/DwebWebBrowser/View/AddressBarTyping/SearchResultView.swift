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
    @ObservedObject var localLinkSearcher = LocalLinkSearcher.shared
    @State private var tapHasBeenHandled = false
    var body: some View {
        Form {
            Section {
                ForEach(0 ..< webSearchers.count, id: \.self) { index in
                    let searcher = webSearchers[index]
                    HStack(spacing: 12) {
                        Image(uiImage: .assetsImage(name: searcher.icon))
                            .resizable()
                            .frame(width: 30, height: 30)
                            .cornerRadius(4)
                            .padding(.leading, 16)

                        VStack(alignment: .leading, spacing: 4, content: {
                            Text(searcher.name)
                                .foregroundColor(Color.menuTitleColor)
                                .font(.system(size: 17))
                                .lineLimit(1)

                            Text(paramURLAbsoluteString(with: addressBar.inputText))
                                .foregroundColor(Color(hexString: "ACB5BF"))
                                .font(.system(size: 12))
                                .lineLimit(1)
                                .padding(.trailing, 8)
                        })
                        Spacer()
                    }
                    .background(Color.menubkColor)
                    .onTapGesture {
                        print("按钮被轻击")
                        guard let url = URL(string: searcher.inputHandler(addressBar.inputText)) else { return }
                        UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
                        openingLink.clickedLink = url
                        addressBar.isFocused = false
                        tapHasBeenHandled = true
                    }
                    .frame(height: 50)
                }
            } header: {
                Text("搜索引擎")
                    .foregroundColor(Color.menuTitleColor)
                    .font(.system(size: 15, weight: .medium))
                    .frame(height: 40)
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
