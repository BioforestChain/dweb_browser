//
//  Bookmark.swift
//  iosApp
//
//  Created by ui06 on 11/21/23.
//  Copyright © 2023 orgName. All rights reserved.
//

import Foundation
import SwiftData

@Model
class Bookmark{
    var link: String
    var iconUrl: String
    var title: String

    init(link: String = emptyLink, iconUrl: String = "baidu", title: String = "about:blank") {
        self.link = link
        self.iconUrl = iconUrl
        self.title = title
    }
    
    static var example = Bookmark(title: "baidu")
}
