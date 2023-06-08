//
//  BookmarkCoreDataManager.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/5/6.
//

import SwiftUI
import CoreData

class BookmarkCoreDataManager {
    
    //增
    func insertBookmark(bookmark: LinkRecord) -> Bool {
        let viewContext = DataController.shared.container.viewContext
        let record = BookmarkEntity(context: viewContext)
        record.id = bookmark.id
        record.imageName = bookmark.imageName
        record.title = bookmark.title
        record.link = bookmark.link
        record.createdDate = bookmark.createdDate
        do {
            try viewContext.save()
        } catch {
            return false
        }
        return true
    }
    
    //删
    func deleteBookmark(uuid: String) {
        
        let viewContext = DataController.shared.container.viewContext
        guard let bookmark = fetchSingleBookmark(uuid: uuid) else { return }
        viewContext.delete(bookmark)
        try? viewContext.save()
                
    }
    //查单个数据
    func fetchSingleBookmark(uuid: String) -> BookmarkEntity? {
        guard let id = UUID(uuidString: uuid) else { return nil }
        let viewContext = DataController.shared.container.viewContext
        let predicate = NSPredicate(format: "id == %@", id as CVarArg)
        let request = BookmarkEntity.fetchRequest()
        request.predicate = predicate
        let result = try? viewContext.fetch(request)
        return result?.first
    }
    
    //查询所有数据
    func fetchTotalBookmarks(offset: Int) async -> [LinkRecord]? {
        
        let viewContext = DataController.shared.container.viewContext
        let request = BookmarkEntity.fetchRequest()
        let sortDesc = NSSortDescriptor(key: "createdDate", ascending: false)
        request.sortDescriptors = [sortDesc]
        request.fetchLimit = 50
        request.fetchOffset = offset
        guard let result = try? viewContext.fetch(request) else { return nil }
        return result.map { entityToBookmark(for: $0) }
    }
    
    //根据内容查找对应数据
    func fetchBookmarkData(with key: String) -> [LinkRecord]? {
        let viewContext = DataController.shared.container.viewContext
        let request = BookmarkEntity.fetchRequest()
        let predicate = NSPredicate(format: "(title CONTAINS %@) OR (link CONTAINS[c] %@)", key, key)
        request.predicate = predicate
        guard let result = try? viewContext.fetch(request) else { return nil }
        return result.map { entityToBookmark(for: $0) }
    }
    
    func entityToBookmark(for entity: BookmarkEntity) -> LinkRecord {
        
        var link = LinkRecord(link: entity.link ?? "", imageName: entity.imageName ?? "", title: entity.title ?? "", createdDate: entity.createdDate)
        link.sectionTime = Date.timeStampToString(stamp: entity.createdDate)
        link.id = entity.id ?? UUID()
        return link
    }
}

