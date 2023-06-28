//
//  DataController.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/4/25.
//

import Foundation
import CoreData

class DataController : ObservableObject {
    
    static let shared = DataController()
    
    private let inMemory: Bool
    
    init(inMemory: Bool = false) {
        self.inMemory = inMemory
        
    }
    
    lazy var container: NSPersistentContainer = {
        let container = NSPersistentContainer(name: "BFS")
        
        guard let description = container.persistentStoreDescriptions.first else {
            fatalError("Failed to retrieve a persistent store description.")
        }
        
        if inMemory {
            description.url = URL(fileURLWithPath: "/dev/null")
        }
        
        description.setOption(true as NSNumber,
                              forKey: NSPersistentStoreRemoteChangeNotificationPostOptionKey)

        description.setOption(true as NSNumber,
                              forKey: NSPersistentHistoryTrackingKey)
        
        container.loadPersistentStores { desc, error in
            if let error = error {
                print("Core Data failed to load: \(error.localizedDescription)")
            }
        }
        
        container.viewContext.automaticallyMergesChangesFromParent = false
        container.viewContext.name = "viewContext"
        container.viewContext.mergePolicy = NSMergeByPropertyObjectTrumpMergePolicy
        container.viewContext.shouldDeleteInaccessibleFaults = true
        return container
    }()
    
    func backgroundContext() -> NSManagedObjectContext {
        
        let context = container.newBackgroundContext()
        context.mergePolicy = NSMergeByPropertyObjectTrumpMergePolicy
        return context
    }
    
    
}
