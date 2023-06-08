//
//  HistoryCoreDataManager.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/5/6.
//

import SwiftUI
import CoreData

class HistoryCoreDataManager {
    
    //增
    func insertHistory(history: LinkRecord) {
        let viewContext = DataController.shared.container.viewContext
        let record = HistoryEntity(context: viewContext)
        record.id = history.id
        record.imageName = history.imageName
        record.title = history.title
        record.link = history.link
        record.createdDate = history.createdDate
        try? viewContext.save()
    }
    
    //删
    func deleteHistory(uuid: String) {
        
        let viewContext = DataController.shared.container.viewContext
        guard let bookmark = fetchSingleHistory(uuid: uuid) else { return }
        viewContext.delete(bookmark)
        try? viewContext.save()
                
    }
    //查单个数据
    func fetchSingleHistory(uuid: String) -> HistoryEntity? {
        guard let id = UUID(uuidString: uuid) else { return nil }
        let viewContext = DataController.shared.container.viewContext
        let predicate = NSPredicate(format: "id == %@", id as CVarArg)
        let request = HistoryEntity.fetchRequest()
        request.predicate = predicate
        let result = try? viewContext.fetch(request)
        return result?.first
    }
    
    //查询所有数据
    func fetchTotalHistories(offset: Int) async -> [LinkRecord]? {
        
        let viewContext = DataController.shared.container.viewContext
        let request = HistoryEntity.fetchRequest()
        let sortDesc = NSSortDescriptor(key: "createdDate", ascending: false)
        request.sortDescriptors = [sortDesc]
        request.fetchLimit = 50
        request.fetchOffset = offset
        guard let result = try? viewContext.fetch(request) else { return nil }
        return result.map { entityToHistory(for: $0) }
    }
    
    //根据内容查找对应数据
    func fetchHistoryData(with key: String) -> [LinkRecord]? {
        let viewContext = DataController.shared.container.viewContext
        let request = HistoryEntity.fetchRequest()
        let predicate = NSPredicate(format: "(title CONTAINS %@) OR (link CONTAINS[c] %@)", key, key)
        request.predicate = predicate
        guard let result = try? viewContext.fetch(request) else { return nil }
        return result.map { entityToHistory(for: $0) }
    }
    
    func entityToHistory(for entity: HistoryEntity) -> LinkRecord {
        
        var link = LinkRecord(link: entity.link ?? "", imageName: entity.imageName ?? "", title: entity.title ?? "", createdDate: entity.createdDate)
        link.sectionTime = Date.timeStampToString(stamp: entity.createdDate)
        link.id = entity.id ?? UUID()
        return link
    }
}
