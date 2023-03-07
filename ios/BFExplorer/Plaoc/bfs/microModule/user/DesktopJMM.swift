//
//  DesktopJMM.swift
//  BFExplorer
//
//  Created by ui08 on 2023/2/24.
//

import Foundation

class DesktopJMM: JsMicroModule {
    convenience init() {
        self.init(metadata: JmmMetadata(id: "desktopuser.dweb", main_url: "file:///bundle/desktop.worker.js"))
    }
}
