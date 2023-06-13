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
    var loadMoreAction: ()->Void

    var body: some View {
        Button {
                print("tap homeViewModel.pageType")

        } label: {
            VStack(alignment: .leading, content: {
                
                HStack(spacing: 12) {
                    WebsiteIconImage(iconUrl: URL(string: linkRecord.websiteIcon) ?? URL.defaultWebIconURL)
                        .frame(width: 28, height: 28)
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
            loadMoreAction()
        }
    }
}

