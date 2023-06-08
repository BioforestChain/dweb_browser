//
//  BookmarkCell.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/5/29.
//

import SwiftUI

struct BookmarkCell: View {
    
    var linkRecord: LinkRecord
    var isLast: Bool
    var isLoadmore: Bool
    
    @ObservedObject var viewModel: HistoryViewModel
    
    @EnvironmentObject var homeViewModel: HomeContentViewModel
    
    var body: some View {
        Button {
            if homeViewModel.pageType == .webPage {
                if homeViewModel.linkString == linkRecord.link {
                    NotificationCenter.default.post(name: NSNotification.Name.hiddenBottomView, object: nil)
                } else {
                    homeViewModel.linkString = linkRecord.link
                    NotificationCenter.default.post(name: NSNotification.Name.loadUrl, object: nil)
                }
            } else {
                homeViewModel.linkString = linkRecord.link
                homeViewModel.pageType = .webPage
            }
            
            homeViewModel.hostString = fetchURLHost(urlString: linkRecord.link)
            
        } label: {
            VStack(alignment: .leading, content: {
                
                HStack(alignment: .center, spacing: 12) {
                    Image(linkRecord.imageName)
                        .frame(width: 28, height: 28)
                        .background(.blue)
                        .padding(.leading, 12)
                        .padding(.top, 11)
                    
                    Text(linkRecord.title)
                        .font(.system(size: 16))
                        .foregroundColor(Color(hexString: "0A1626"))
                        .frame(height: 20)
                        .padding(.top, 15)
                        .lineLimit(1)
                }
                
                Spacer()
                
                if !isLast {
                    Rectangle()
                        .frame(height: 0.5)
                        .foregroundColor(Color(hexString: "E8EBED"))
                        .padding(.bottom, 1)
                }
            })
            .background(.clear)
        }
        .onAppear {
            guard isLast else { return }
            viewModel.loadMoreHistoryData()
        }
    }
}

