//
//  NoResultView.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/4/23.
//

import SwiftUI

enum EmptyType {
    case bookmark, history
    
    var image: UIImage { .assetsImage(name: self == .bookmark ? "bookmark_empty":"history_empty") }
    var msg: String { self == .bookmark ? "暂无书签":"暂无记录" }
}

struct NoResultView: View {
    var empty: EmptyType
    @EnvironmentObject var dragScale: WndDragScale
    var body: some View {
        
        ZStack {
            Color.bkColor
                .edgesIgnoringSafeArea(.top)
            
            VStack(spacing: 24, content: {
                Image(uiImage: empty.image)
                    .resizable()
                    .scaledToFit()
                    .frame(width: dragScale.properValue(floor: 50, ceiling: 120), height: dragScale.properValue(floor: 50, ceiling: 120))

                Text(empty.msg)
                    .font(.system(size: dragScale.scaledFontSize(maxSize: 22)))
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
