//
//  HistoryCell.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/5/5.
//

import SwiftUI

struct HistoryCell: View {
    
    var linkRecord: LinkRecord
    var isLast: Bool
//    var isLoadmore: Bool
    
    
    var clickLoadMore: ()->Void
//    @ObservedObject var viewModel: HistoryViewModel
    
//    @EnvironmentObject var homeViewModel: HomeContentViewModel
    
    var body: some View {
        Button {
//            if homeViewModel.pageType == .webPage {
//                if homeViewModel.linkString == linkRecord.link {
//                    NotificationCenter.default.post(name: NSNotification.Name.hiddenBottomView, object: nil)
//                } else {
//                    homeViewModel.linkString = linkRecord.link
//                    NotificationCenter.default.post(name: NSNotification.Name.loadUrl, object: nil)
//                }
            print("opening a history link\(linkRecord.link)")
//            } else {
//                homeViewModel.linkString = linkRecord.link
//                homeViewModel.pageType = .webPage
//            }
            
//            homeViewModel.hostString = fetchURLHost(urlString: linkRecord.link)
            
        } label: {
            VStack(alignment: .leading, content: {
                Text(linkRecord.title)
                    .font(.system(size: 16))
                    .foregroundColor(Color(hexString: "0A1626"))
                    .frame(height: 20)
                    .lineLimit(1)
                    .padding(.top, 14)
                    .padding(.horizontal, 16)
                
                Text(linkRecord.link)
                    .font(.system(size: 11))
                    .foregroundColor(Color(hexString: "ACB5BF"))
                    .frame(height: 14)
                    .lineLimit(1)
                    .padding(.horizontal, 16)
                
                Spacer()
                
                if !isLast {
                    Rectangle()
                        .frame(height: 0.5)
                        .foregroundColor(Color(hexString: "E8EBED"))
                        .padding(.bottom, 1)
                }
            })
            
            .frame(height: 66)
            .background(.clear)
        }
        .onAppear {
            guard isLast else { return }
            clickLoadMore()
//            viewModel.loadMoreHistoryData()
        }
    }
}
