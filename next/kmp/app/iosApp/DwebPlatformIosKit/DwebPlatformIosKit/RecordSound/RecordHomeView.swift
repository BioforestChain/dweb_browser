//
//  RecordHomeView.swift
//  DwebPlatformIosKit
//
//  Created by ios on 2024/1/29.
//

import SwiftUI
import CoreData

struct RecordHomeView: View {
    
    @StateObject private var dataController = SoundDataController.shared
    var multiple: Bool
    var limit: Int
    
    var body: some View {
        RecordMainView(multiple: multiple, limit: limit)
            .background(Color(uiColor: bkColor))
            .environment(\.managedObjectContext, dataController.container.viewContext)
    }
}

