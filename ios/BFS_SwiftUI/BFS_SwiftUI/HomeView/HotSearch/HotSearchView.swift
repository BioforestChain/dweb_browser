//
//  HotSearchView.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/4/24.
//

import SwiftUI

struct HotSearchView: View {
    
    let scales = [0.3, 0.8, 0.32, 0.62, 0.4, 0.72, 0.82, 0.33, 0.6, 0.42, 0.7, 0.5]
    @FetchRequest(sortDescriptors: [NSSortDescriptor(key: "index", ascending: true)]) var hotList: FetchedResults<HotSearchEntity>
    @ObservedObject var manager = HotSearchDataManager()
    
    var body: some View {
        
        VStack(alignment: .leading) {
            Text("全网热搜")
                .font(.system(size: 21, weight: .bold))
                .frame(height: 40)
                .padding(.leading, 20)
            
            if hotList.count > 0 {
                ForEach(hotList, id: \.id) { model in
                    HotSearchCell(hotModel: HotSearchModel(hot: model))
                        .frame(height: 30)
                }
                .padding(.leading, 20)
            } else {
                ForEach(scales, id: \.self) { scale in
                    GeometryReader { proxy in
                        ZStack(alignment: .leading, content: {
                            Rectangle()
                                .fill(.clear)
                                .frame(height: 30)
                            HotSearchHoverView()
                                .cornerRadius(3)
                                .frame(width: scale * proxy.size.width,height: 24)
                        })
                    }
                    
                    .padding(8)
                    
                }
                .padding(EdgeInsets(top: 0, leading: 20, bottom: 10, trailing: 0))
            }
        }
        .onAppear {
            manager.loadHotSearchData()
        }
    }
}


struct HotSearchView_Previews: PreviewProvider {
    static var previews: some View {
       
        
        HotSearchView()
    }
}
