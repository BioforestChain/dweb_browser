//
//  NoResultView.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/4/23.
//

import SwiftUI

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
