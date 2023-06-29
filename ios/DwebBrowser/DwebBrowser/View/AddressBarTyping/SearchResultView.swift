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
    @ObservedObject var localLinkSearcher = LocalLinkSearcher.shared
    @State private var inputText: String = ""
    var body: some View {
        
        Form {
            Section {
                ForEach(WebSearcher.shared.searchers, id: \.id) { searcher in
                    Button {
                        printDate()
                        guard let url = URL(string: searcher.inputHandler("sss")) else { return }
                        DispatchQueue.main.async {
                            openingLink.clickedLink = url
                            addressBar.isFocused = false
                        }
                    } label: {
                        VStack {
                            HStack(spacing: 12) {
                                Image(uiImage: .assetsImage(name: (searcher.icon)))
                                    .resizable()
                                    .frame(width: 30, height: 30)
                                    .padding(.leading, 16)
                                    .padding(.top, 10)
                                
                                VStack(alignment: .leading, spacing: 4, content: {
                                    Text(searcher.name)
                                        .foregroundColor(Color(hexString: "0A1626"))
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
                    .foregroundColor(Color(hexString: "0A1626"))
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
                        DispatchQueue.main.async {
                            openingLink.clickedLink = url
                            addressBar.isFocused = false
                        }
                        
                    } label: {
                        HStack {
                            Image(uiImage: .assetsImage(name: (record.websiteIcon)))
                                .foregroundColor(SwiftUI.Color.init(white: 138 / 255))
                                .frame(width: 22, height: 22)
                            VStack(alignment: .leading, spacing: 4, content: {
                                Text(record.title)
                                    .font(.system(size: 16))
                                    .foregroundColor(.black)
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
//        header: {
//            Text("本地记录")
//                .foregroundColor(Color(hexString: "0A1626"))
//                .font(.system(size: 15, weight: .medium))
//                .frame(height: 40)
//        }
//        .textCase(nil)
//        .listRowInsets(EdgeInsets())
//        .listRowSeparator(.hidden)
            .onReceive(addressBar.$inputText){ text in
                localLinkSearcher.fetchRecordList(placeHolder: text)

            }
        }
        .dismissKeyboard()
    }
}



