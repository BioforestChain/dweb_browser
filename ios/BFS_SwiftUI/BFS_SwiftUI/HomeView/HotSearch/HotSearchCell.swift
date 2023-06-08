//
//  HotSearchCell.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/4/25.
//

import SwiftUI

struct HotSearchCell: View {
    
    var hotModel: HotSearchModel
    @EnvironmentObject var homeViewModel: HomeContentViewModel
    
    private let colors = [Color(hexString: "FA3E3E"), Color(hexString: "FA9C3E"), Color(hexString: "FA9C3E"), Color(hexString: "ACB5BF")]
    
    var body: some View {
        HStack {
            Button {
                homeViewModel.pageType = .webPage
                homeViewModel.linkString = hotModel.link
                homeViewModel.hostString = fetchURLHost(urlString: hotModel.link)
            } label: {
                HStack() {
                    Text(" \(hotModel.index) ")
                        .font(.system(size: 18.0, weight: .bold))
                        .foregroundColor(colors[hotModel.index < 4 ? hotModel.index - 1 : 3])
                        .multilineTextAlignment(.leading)
                    Text("\(hotModel.title)")
                        .font(.system(size: 16.0, weight: .regular))
                        .foregroundColor(Color(hexString: "0a1626"))
                        .multilineTextAlignment(.leading)
                        .lineLimit(1)
                    Spacer()
                }
            }
            Spacer()
        }
    }
}

struct HotSearchCell_Previews: PreviewProvider {
    static var previews: some View {
        HotSearchCell(hotModel: HotSearchModel(link: "", title: "哈哈酒店五一出售大厅睡沙发99元一晚， 客服已宝贝", index: 2))
    }
}


