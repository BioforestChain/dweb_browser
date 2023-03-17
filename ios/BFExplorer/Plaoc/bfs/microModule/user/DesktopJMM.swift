//
//  DesktopJMM.swift
//  BFExplorer
//
//  Created by ui08 on 2023/2/24.
//

import Foundation

class DesktopJMM: JsMicroModule {
    convenience init() {
        self.init(metadata: JmmMetadata(
            id: "desktop.user.dweb",
            server: .init(root: "file:///bundle", entry: "/desktop.worker.js")))
    }
}
