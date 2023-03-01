//
//  DesktopJMM.swift
//  BFExplorer
//
//  Created by ui08 on 2023/2/24.
//

import Foundation

class DesktopJMM: JsMicroModule {
    convenience init() {
        let mmid: Mmid = "desktop.user.dweb"
        let metadata = JmmMetadata(main_url: "file:///bundle/desktop.worker.js")
        self.init(mmid: mmid, metadata: metadata)
    }
}
