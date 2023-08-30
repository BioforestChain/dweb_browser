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

    var body: some View {
            Form {
                Section {
                    ForEach(0..<WebSearcher.shared.searchers.count, id: \.self) { index in
                        let searcher = WebSearcher.shared.searchers[index]
                        Button {
                            guard let url = URL(string: searcher.inputHandler(addressBar.inputText)) else { return }
                            UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
                            openingLink.clickedLink = url
                            addressBar.isFocused = false
                        } label: {
                            VStack {
                                HStack(spacing: 12) {
                                    Image(uiImage: .assetsImage(name: searcher.icon))
                                        .resizable()
                                        .frame(width: 30, height: 30)
                                        .cornerRadius(4)
                                        .padding(.leading, 16)
                                        .padding(.top, 10)
                                    
                                    VStack(alignment: .leading, spacing: 4, content: {
                                        Text(searcher.name)
                                            .foregroundColor(Color.menuTitleColor)
                                            .font(.system(size: 17))
                                            .lineLimit(1)
                                            .padding(.top, 16)
                                        
                                        Text(paramURLAbsoluteString(with: addressBar.inputText))
                                            .foregroundColor(Color(hexString: "ACB5BF"))
                                            .font(.system(size: 12))
                                            .lineLimit(1)
                                            .padding(.trailing, 16)
                                    })
                                    Spacer()
                                }
                                Spacer()
                                Rectangle()
                                    .frame(height: 0.5)
                                    .foregroundColor(index == WebSearcher.shared.searchers.count - 1 ? Color.clear : Color.lineColor)
                                    .padding(.bottom, 0)
                                    .padding(.leading, 16)
                                    
                            }
                            .background(Color.menubkColor)
                        }
                        .frame(height: 70)
                    }
                } header: {
                    Text("搜索引擎")
                        .foregroundColor(Color.menuTitleColor)
                        .font(.system(size: 15, weight: .medium))
                        .frame(height: 40)
                }
                .textCase(nil)
                .listRowInsets(EdgeInsets())
                .listRowSeparator(.hidden)
                
                Section {
                    ForEach(0..<localLinkSearcher.records.count, id: \.self) { index in
                        let record = localLinkSearcher.records[index]
                        Button {
                            guard let url = URL(string: record.link) else { return }
                            UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
                            DispatchQueue.main.async {
                                openingLink.clickedLink = url
                                addressBar.isFocused = false
                            }
                            
                        } label: {
                            VStack {
                                HStack(spacing: 12) {
                                    Image(uiImage: .assetsImage(name: record.websiteIcon))
                                        .resizable()
                                        .frame(width: 30, height: 30)
                                        .cornerRadius(4)
                                        .foregroundColor(Color.ToolbarColor)
                                        .padding(.leading, 16)
                                        .padding(.top, 10)
                                    
                                    VStack(alignment: .leading, spacing: 4, content: {
                                        Text(record.title)
                                            .foregroundColor(Color.menuTitleColor)
                                            .font(.system(size: 16))
                                            .lineLimit(1)
                                            .padding(.top, 16)
                                        
                                        Text(record.link)
                                            .foregroundColor(Color(hexString: "ACB5BF"))
                                            .font(.system(size: 13))
                                            .lineLimit(1)
                                            .padding(.trailing, 16)
                                    })
                                    Spacer()
                                }
                                Spacer()
                                Rectangle()
                                    .frame(height: 0.5)
                                    .foregroundColor(index == localLinkSearcher.records.count - 1 ? Color.clear : Color.lineColor)
                                    .padding(.bottom, 0)
                                    .padding(.leading, 16)
                            }
                            .background(Color.menubkColor)
                        }
                        .frame(height: 70)
                    }
                }
                .textCase(nil)
                .listRowInsets(EdgeInsets())
                .listRowSeparator(.hidden)
                
                .onReceive(addressBar.$inputText) { text in
                    localLinkSearcher.fetchRecordList(placeHolder: text)
                }
            }
            .scrollContentBackground(.hidden)
            .background(Color.bkColor)
        }
    
}
