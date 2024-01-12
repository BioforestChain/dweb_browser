//
//  DwebDeepLink.swift
//  iosApp
//
//  Created by bfs-kingsword09 on 2023/11/29.
//  Copyright © 2023 orgName. All rights reserved.
//

import Foundation
import DwebShared

class DwebDeepLink {
    static let shared = DwebDeepLink()
    
    func openDeepLink(url: String) {
        Main_iosKt.dwebDeepLinkHook.emitOnInit(url: url)
    }
}
