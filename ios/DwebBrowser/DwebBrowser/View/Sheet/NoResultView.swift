//
//  NoResultView.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/4/23.
//

import SwiftUI

struct NoResultView: View {
    var config: LinkConfig
    var body: some View {
        
        ZStack {
            Color.bkColor
                .edgesIgnoringSafeArea(.top)
            
            VStack(spacing: 24, content: {
                Image(uiImage: .assetsImage(name: (config.imageName)))
                    .resizable()
                    .scaledToFit()
                    .frame(width: 120, height: 120)
                Text(config.msg)
                    .font(.system(size: 22.0))
                    .foregroundColor(Color(hexString: "c9c9c9"))
                    .lineLimit(1)
            })
        }
    }
}

struct NoResultView_Previews: PreviewProvider {
    static var previews: some View {
        NoResultView(config: .history)
    }
}
