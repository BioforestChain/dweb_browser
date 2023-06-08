//
//  BFS_SwiftUIApp.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/4/19.
//

import SwiftUI

@main
struct BFS_SwiftUIApp: App {
    
    var body: some Scene {
        WindowGroup {
            DownloadAppView().environment(\.managedObjectContext, DataController.shared.container.viewContext)
        }
    }
}
