//
//  HotWebsiteData.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/4/24.
//

import SwiftUI

class HotWebsiteData: ObservableObject {
    
    @Published var hotWebsites: [WebModel] = []
    
    private var images: [String] {
        return ["douyu","weibo","zhihu","tengxun","wangyi","douban","bilibili","jingdong"]
    }
    
    private var titles: [String] {
        return ["斗鱼","微博","知乎","腾讯新闻","网易","钱包","哔哩哔哩","京东"]
    }
    
    private var links: [String] {
        return ["http://www.douyu.com","http://www.weibo.com","http://www.zhihu.com","http://www.qq.com/","http://www.163.com","http://localhost:8000/index","http://www.bilibili.com","http://www.jd.com"]
    }
    
    init() {
        for i in 0..<images.count {
            let model = WebModel(icon: images[i], title: titles[i], link: links[i])
            hotWebsites.append(model)
        }
    }
}
