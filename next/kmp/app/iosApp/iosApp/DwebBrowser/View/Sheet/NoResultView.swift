//
//  NoResultView.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/4/23.
//

import SwiftUI

struct LinkConfig {
    var title: String = ""
    var type: String = ""
    var imageName: String = ""
    var msg: String = ""
    
    static var bookmark = LinkConfig(title: "搜索书签", type: "书签", imageName: "bookmark_empty", msg: "暂无书签" )
    static var history = LinkConfig(title: "搜索历史记录", type: "历史记录", imageName: "history_empty", msg:"暂无记录" )
}


struct NoResultView: View {
    var config: LinkConfig
    @EnvironmentObject var dragScale: WndDragScale
    var body: some View {
        
        ZStack {
            Color.bkColor
                .edgesIgnoringSafeArea(.top)
            
            VStack(spacing: 24, content: {
                Image(uiImage: .assetsImage(name: (config.imageName)))
                    .resizable()
                    .scaledToFit()
                    .frame(width: dragScale.properValue(floor: 50, ceiling: 120), height: dragScale.properValue(floor: 50, ceiling: 120))

                Text(config.msg)
                    .font(.system(size: dragScale.scaledFontSize(maxSize: 22)))
                    .foregroundColor(Color(hexString: "c9c9c9"))
            })
        }
    }
}

struct NoResultView_Previews: PreviewProvider {
    static var previews: some View {
        NoResultView(config: .history)
    }
}
