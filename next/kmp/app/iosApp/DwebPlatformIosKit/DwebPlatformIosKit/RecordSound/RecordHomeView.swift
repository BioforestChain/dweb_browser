//
//  RecordHomeView.swift
//  DwebPlatformIosKit
//
//  Created by ios on 2024/1/29.
//

import SwiftUI
import CoreData

struct RecordHomeView: View {
    
    @StateObject private var dataController = DataController()
    var body: some View {
        RecordMainView()
            .background(Color(uiColor: bkColor))
            .environment(\.managedObjectContext, dataController.container.viewContext)
    }
}

#Preview {
    RecordHomeView()
}
