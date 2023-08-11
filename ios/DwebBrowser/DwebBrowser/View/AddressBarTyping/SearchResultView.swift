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
    //    @State private var inputText: String = ""
    var body: some View {
        Form {
            Section {
                ForEach(WebSearcher.shared.searchers, id: \.id) { searcher in
                    Button {
                        guard let url = URL(string: searcher.inputHandler(addressBar.inputText)) else { return }
                        UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
                        let webcache = WebCacheMgr.shared.store[selectedTab.curIndex]
                        if !webcache.shouldShowWeb {
                            webcache.shouldShowWeb = true
                        }
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
                                        .foregroundColor(Color("menuTitleColor"))
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
                                .foregroundColor(Color(hexString: "E8EBED"))
                                .padding(.bottom, 0)
                        }
                    }
                    .frame(height: 70)
                }
            } header: {
                Text("搜索引擎")
                    .foregroundColor(Color("menuTitleColor"))
                    .font(.system(size: 15, weight: .medium))
                    .frame(height: 40)
            }
            .textCase(nil)
            .listRowInsets(EdgeInsets())
            .listRowSeparator(.hidden)
            
            Section {
                ForEach(localLinkSearcher.records) { record in
                    Button {
                        guard let url = URL(string: record.link) else { return }
                        UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
                        DispatchQueue.main.async {
                            openingLink.clickedLink = url
                            addressBar.isFocused = false
                        }
                        
                    } label: {
                        HStack {
                            Image(uiImage: .assetsImage(name: record.websiteIcon))
                                .resizable()
                                .foregroundColor(Color(white: 138.0 / 255.0))
                                .frame(width: 28, height: 28)
                            VStack(alignment: .leading, spacing: 4, content: {
                                Text(record.title)
                                    .font(.system(size: 16))
                                    .foregroundColor(Color("menuTitleColor"))
                                    .frame(height: 25)
                                    .lineLimit(1)
                                    .padding(.top, 10)
                                Text(record.link)
                                    .font(.system(size: 13))
                                    .foregroundColor(.init(white: 0.667))
                                    .frame(height: 20)
                                    .lineLimit(1)
                                    .padding(.trailing, 16)
                            })
                        }
                    }
                }
            }
            .onReceive(addressBar.$inputText) { text in
                localLinkSearcher.fetchRecordList(placeHolder: text)
            }
        }
        //        .dismissKeyboard()
    }
}
