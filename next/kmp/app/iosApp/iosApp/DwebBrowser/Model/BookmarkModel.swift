//
//  BookmarkModel.swift
//  iosApp
//
//  Created by ui06 on 11/29/23.
//  Copyright © 2023 orgName. All rights reserved.
//

import Foundation
import SwiftData

@Model
class Bookmark{
    var link: String
    var iconUrl: String
    var title: String

    init(link: String = "https:www.apple.com", iconUrl: String = "https://www.apple.com/favicon.ico", title: String = "Apple") {
        self.link = link
        self.iconUrl = iconUrl
        self.title = title
    }
    
    static var example = Bookmark()
}
