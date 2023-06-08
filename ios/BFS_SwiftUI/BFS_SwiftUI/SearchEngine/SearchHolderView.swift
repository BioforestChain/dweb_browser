//
//  SearchHolderView.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/5/8.
//

import SwiftUI

struct SearchHolderView: View {
    
    private let titles: [String] = ["百度","搜狗","360"]
    private let images: [String] = ["baiduIcon","sougoushuru","360"]
    
    @EnvironmentObject var homeViewModel: HomeContentViewModel
    
    var body: some View {
        
        Form {
            Section {
                ForEach(0..<titles.count, id: \.self) { index in
                    Button {
                        var type: Engine = .baidu
                        switch index {
                        case 0:
                            type = .baidu
                        case 1:
                            type = .sogou
                        case 2:
                            type = .engine360
                        default:
                            break
                        }
                        let urlString = searchContent(for: type, text: paramURLAbsoluteString(with: homeViewModel.linkPlaceHolderString))
                        clickWebContent(urlString: urlString)
                    } label: {
                        VStack {
                            HStack(spacing: 12) {
                                Image(images[index])
                                    .frame(width: 30, height: 30)
                                    .padding(.leading, 16)
                                    .padding(.top, 10)
                                    
                                VStack(alignment: .leading, spacing: 4, content: {
                                    Text(titles[index])
                                        .foregroundColor(Color(hexString: "0A1626"))
                                        .font(.system(size: 17))
                                        .lineLimit(1)
                                        .padding(.top, 16)
                                    
                                    Text(paramURLAbsoluteString(with: homeViewModel.linkPlaceHolderString))
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
                                .padding(.bottom, 1)
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
                ForEach(SearchEngineViewModel.shared.records) { record in
                    Button {
                        clickWebContent(urlString: record.link)
                    } label: {
                        HStack {
                            Image(record.imageName)
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
        }
        .dismissKeyboard()
    }
    
    private func clickWebContent(urlString: String) {
        homeViewModel.linkString = urlString
        homeViewModel.hostString = fetchURLHost(urlString: urlString)
        homeViewModel.isShowEngine = false
        homeViewModel.isShowOverlay = false
        homeViewModel.pageType = .webPage
        UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
        NotificationCenter.default.post(name: NSNotification.Name.loadUrl, object: nil)
    }
}

struct SearchHolderView_Previews: PreviewProvider {
    static var previews: some View {
        SearchHolderView()
    }
}
