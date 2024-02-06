//
//  NoResultView.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/4/23.
//

import SwiftUI

enum EmptyType {
    case bookmark, history
    
    var image: UIImage { self == .bookmark ? UIImage(resource: .bookmarkEmpty) : UIImage(resource: .historyEmpty) }
    var msg: String { self == .bookmark ? "暂无书签":"暂无记录" }
}

struct NoResultView: View {
    var empty: EmptyType
    @Environment(WndDragScale.self) var dragScale
    var body: some View {
        
        ZStack {
            Color.bk
                .edgesIgnoringSafeArea(.top)
            
            VStack(spacing: 24, content: {
                Image(uiImage: empty.image)
                    .resizable()
                    .scaledToFit()
                    .frame(width: dragScale.properValue(max: 120), height: dragScale.properValue(max: 120))

                Text(empty.msg)
                    .font(dragScale.scaledFont_22)
                    .foregroundColor(Color(.systemGray))
            })
        }
    }
}

struct NoResultView_Previews: PreviewProvider {
    static var previews: some View {
        NoResultView(empty: .history)
    }
}
