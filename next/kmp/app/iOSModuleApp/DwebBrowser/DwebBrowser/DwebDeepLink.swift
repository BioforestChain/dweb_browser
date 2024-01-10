//
//  DwebDeepLink.swift
//  iosApp
//
//  Created by bfs-kingsword09 on 2023/11/29.
//  Copyright © 2023 orgName. All rights reserved.
//

import Foundation
import DwebShared

public class DwebDeepLink {
    public static let shared = DwebDeepLink()
    public func openDeepLink(url: String) {
        Main_iosKt.dwebDeepLinkHook.emitOnInit(url: url)
    }
}
