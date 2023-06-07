//
//  ShowHomePageViewModel.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/6/7.
//

import SwiftUI

class ShowHomePageViewModel: ObservableObject {
    
    enum PageType {
        case homePage
        case webPage
    }
    
    @Published var linkString = ""
    @Published var hostString = ""
    @Published var linkPlaceHolderString = ""
    @Published var webTitleString = ""
    @Published var pageType: PageType = .homePage
    @Published var isShowEngine = false      //是否显示搜索引擎视图
    @Published var isShowOverlay: Bool = false      //是否显示覆盖层
    @Published var isPlaceholderObserver: Bool = false      //是否监听placeholder的变化
    @Published var bottom_disabledList = [true,true,true,false,false]
}
