//
//  DataController.swift
//  DwebPlatformIosKit
//
//  Created by ios on 2024/1/29.
//

import Foundation
import CoreData

class SoundDataController: ObservableObject {
    
    static let shared = SoundDataController()
    
    init() {
        
    }
    
    lazy var container: NSPersistentContainer = {
        let bundle = Bundle(for: RecordManager.self)
        let modelURL = bundle.url(forResource: "RecordSound", withExtension: "momd")
        if modelURL == nil {
            fatalError("Failed to load URL")
        }
        
        let objModel = NSManagedObjectModel.init(contentsOf: modelURL!)
        if objModel == nil {
            fatalError("Failed to load objModel")
        }
        
        let container = NSPersistentContainer(name: "RecordSound", managedObjectModel: objModel!)
        container.loadPersistentStores { description, error in
            if let error = error {
                print("Core Data failed to load: \(error.localizedDescription)")
            }
        }
        return container
    }()
    
}
