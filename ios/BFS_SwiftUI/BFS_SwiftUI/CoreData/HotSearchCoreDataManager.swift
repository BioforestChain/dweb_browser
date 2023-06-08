//
//  HotSearchCoreDataManager.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/4/25.
//

import SwiftUI
import CoreData

class HotSearchCoreDataManager {
    
    private var lastToken: NSPersistentHistoryToken?
    private var notificationToken: NSObjectProtocol?
    
    init() {
        notificationToken = NotificationCenter.default.addObserver(forName: .NSPersistentStoreRemoteChange, object: nil, queue: nil) { note in
            Task {
                await self.fetchHotSearchData()
            }
        }
    }
    
    deinit {
        if let observer = notificationToken {
            NotificationCenter.default.removeObserver(observer)
        }
    }
    
    //插入热搜的数据
    func addHotSearchBatch(with hotSearchs: [HotSearchModel]) async throws {
        guard hotSearchs.count > 0 else { return }
        let context = DataController.shared.backgroundContext()
        try await context.perform({
            let batchRequest = self.newBatchInsertRequest(with: hotSearchs)
            batchRequest.resultType = .statusOnly
            let result = try? context.execute(batchRequest) as? NSBatchInsertResult
            let success = result?.result as? Bool ?? false
            if success {
                 return
            }
            throw CoreDataError.batchInsertError
        })
    }
    
    private func newBatchInsertRequest(with hotSearchs: [HotSearchModel]) -> NSBatchInsertRequest {
        
        var index = 0
        let batchInsert = NSBatchInsertRequest(entity: HotSearchEntity.entity()) { (managedObject: NSManagedObject) -> Bool in
            
            guard index < hotSearchs.count else { return true }
            if let hotSearch = managedObject as? HotSearchEntity {
                let model = hotSearchs[index]
                hotSearch.id = model.id
                hotSearch.title = model.title
                hotSearch.link = model.link
                hotSearch.index = Int16(model.index)
            }
            index += 1
            return false
        }
        return batchInsert
    }
     
    //删除所有热搜的数据
    func deleteTotalHotSearchBatch() async throws {
        
        let context = DataController.shared.backgroundContext()
        return try await context.perform({
            let request = NSFetchRequest<NSFetchRequestResult>(entityName: "HotSearchEntity")
            let batchDeleteRequest = NSBatchDeleteRequest(fetchRequest: request)
            batchDeleteRequest.resultType = .resultTypeStatusOnly
            let result = try context.execute(batchDeleteRequest) as? NSBatchDeleteResult
            if result?.result as? Bool ?? false {
                return
            }
            throw CoreDataError.batchDeleteError
        })
    }
    //根据条件删除数据
    func deleteHotSearchList(_ hotSearchs: [HotSearchModel]) async throws {
        
        let entitys = HotSearchListToEntiryList(for: hotSearchs)
        let objectIDs = entitys.map { $0.objectID }
        let taskContext = DataController.shared.backgroundContext()
        try await taskContext.perform {
            let batchDeleteRequest = NSBatchDeleteRequest(objectIDs: objectIDs)
            let fetchResult = try? taskContext.execute(batchDeleteRequest) as? NSBatchDeleteResult
            if let success = fetchResult?.result as? Bool, success {
                return
            }
            throw CoreDataError.batchDeleteError
        }
    }
    
    //查找热搜的数据
    func fetchHotSearchData() async {
        do {
            try await fetchHotSearchDataTransactionsAndChanges()
        } catch {
            print(error.localizedDescription)
        }
    }
    
    private func fetchHotSearchDataTransactionsAndChanges() async throws {
        
        let taskContext = DataController.shared.backgroundContext()
        try await taskContext.perform {
            let changeRequest = NSPersistentHistoryChangeRequest.fetchHistory(after: self.lastToken)
            let result = try taskContext.execute(changeRequest) as? NSPersistentHistoryResult
            if let history = result?.result as? [NSPersistentHistoryTransaction], !history.isEmpty {
                self.mergeHotSearchDataChanges(from: history)
                return
            }
            throw CoreDataError.persistentHistoryChangeError
        }
    }
    
    private func mergeHotSearchDataChanges(from history: [NSPersistentHistoryTransaction]) {
        
        let viewContext = DataController.shared.container.viewContext
        viewContext.perform {
            for transaction in history {
                viewContext.mergeChanges(fromContextDidSave: transaction.objectIDNotification())
                self.lastToken = transaction.token
            }
        }
    }
    
    //模型转换
    private func HotSearchListToEntiryList(for hotSearchs: [HotSearchModel]) -> [HotSearchEntity] {
        
        guard hotSearchs.count > 0 else { return [] }
        var entityList: [HotSearchEntity] = []
        for hotSearchModel in hotSearchs {
            let hotSearch = HotSearchEntity(context: DataController.shared.container.viewContext)
            hotSearch.title = hotSearchModel.title
            hotSearch.link = hotSearchModel.link
            hotSearch.id = hotSearchModel.id
            hotSearch.index = Int16(hotSearchModel.index)
            entityList.append(hotSearch)
        }
        return entityList
    }
}
