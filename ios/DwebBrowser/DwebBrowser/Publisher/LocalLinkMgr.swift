//
//  BookmarkMgr.swift
//  DwebBrowser
//
//  Created by ui06 on 6/13/23.
//

//Management of bookmark and visit history

import Foundation


//struct UniqueGroup<ID, ELement>: Identifiable where ID: Hashable {
//
//    let id: ID
//    let items: [ELement]
//}
//
//class HistoryViewModel: ObservableObject {
//    static var shared = HistoryViewModel()
//
//    @Published var sections: [UniqueGroup<String, LinkRecord>] = []
//    private var searchText: String = ""
//
//    var list: [LinkRecord] = [.example]
//
//    var searchResults: [LinkRecord] {
//        if searchText.isEmpty {
//            return list
//        } else {
//            return list.filter { $0.title.contains(searchText) ||  $0.link.lowercased().contains(searchText.lowercased()) }
//        }
//    }
//
//    func groupedSep() {
//        let historyDict = Dictionary(grouping: searchResults, by: { $0.sectionTime})
//        self.sections = historyDict.map { UniqueGroup(id: $0.key, items: $0.value.sorted(by: { $0.createdDate > $1.createdDate
//        })) }.sorted(by: {$0.id > $1.id} )
//    }
//
//    //搜索
//    func searchHistory(for searchText: String) {
//
//        self.searchText = searchText
//        groupedSep()
//    }
//
//    //删除数据
//    func deleteSingleHistory(for uuid: String) {
//        if let index = list.firstIndex(where: { $0.id.uuidString == uuid }) {
//            list.remove(at: index)
//            groupedSep()
//        }
//    }
//
//    //删除数据
//    func deleteHistory(for uuids: [String]) {
//        list.removeAll { uuids.contains($0.id.uuidString) }
//        groupedSep()
//    }
//
//    //分页加载
//    func loadMoreHistoryData() { }
//}
//
//
//protocol LinkOperator: {
//    func fetchItems(range: NSRange)
//
//    func deleteItems(collection: [String])
//    func appendItems(collection: [String])
//
//}
//
//
//final class LocalLinkMgr: HistoryViewModel {
//
//    @Published var dataSources: [LinkRecord] = []
//    private let coredataManager = BookmarkCoreDataManager()
//    override init() {
//        super.init()
//        DispatchQueue.main.async {
//            self.loadHistoryData()
//        }
//    }
//
//    private func loadHistoryData() {
//        Task {
//            dataSources = await coredataManager.fetchTotalBookmarks(offset: 0) ?? [.example]
//        }
//    }
//
//    override func deleteSingleHistory(for uuid: String) {
//        super.deleteSingleHistory(for: uuid)
//        coredataManager.deleteBookmark(uuid: uuid)
//    }
//
//    override func groupedSep() {
//
//    }
//
//    override func loadMoreHistoryData()  {
//
//        Task {
//            await loadMoreBookmarkDataFromCoreData()
//            DispatchQueue.main.async {
//                self.groupedSep()
//            }
//        }
//    }
//
//    private func loadMoreBookmarkDataFromCoreData() async {
//        guard let oldData = await coredataManager.fetchTotalBookmarks(offset: list.count),
//              oldData.count > 0 else {
//            return
//        }
//        list += oldData
//    }
//}




//======
//
//class Person: NSManagedObject {
//    @NSManaged var name: String
//    @NSManaged var age: Int
//}
//
//
//class PersonDataManager {
//    let persistentContainer: NSPersistentContainer
//
//    init() {
//        persistentContainer = NSPersistentContainer(name: "PersonDataModel")
//        persistentContainer.loadPersistentStores { _, error in
//            if let error = error {
//                fatalError("Failed to load Core Data stack: \(error)")
//            }
//        }
//    }
//
//    func createPerson(name: String, age: Int) {
//        let context = persistentContainer.viewContext
//        let person = Person(context: context)
//        person.name = name
//        person.age = Int16(age)
//
//        do {
//            try context.save()
//            print("Person created successfully.")
//        } catch {
//            print("Failed to create person: \(error)")
//        }
//    }
//
//    func deletePerson(person: Person) {
//        let context = persistentContainer.viewContext
//        context.delete(person)
//
//        do {
//            try context.save()
//            print("Person deleted successfully.")
//        } catch {
//            print("Failed to delete person: \(error)")
//        }
//    }
//
//    func updatePerson(person: Person, newName: String, newAge: Int) {
//        let context = persistentContainer.viewContext
//        person.name = newName
//        person.age = Int16(newAge)
//
//        do {
//            try context.save()
//            print("Person updated successfully.")
//        } catch {
//            print("Failed to update person: \(error)")
//        }
//    }
//
//    func retrievePersons() -> [Person] {
//        let context = persistentContainer.viewContext
//        let fetchRequest: NSFetchRequest<Person> = Person.fetchRequest()
//
//        do {
//            let persons = try context.fetch(fetchRequest)
//            return persons
//        } catch {
//            print("Failed to retrieve persons: \(error)")
//            return []
//        }
//    }
//}
//+++++++++++++++++++++++++++

//
//import CoreData
//
//class HistoryEntity: NSManagedObject {
//    @NSManaged var id: String
//    @NSManaged var imageName: String
//    @NSManaged var title: String
//    @NSManaged var link: String
//    @NSManaged var createdDate: Date
//}
//
//class BookmarkEntity: NSManagedObject {
//    @NSManaged var id: String
//    @NSManaged var imageName: String
//    @NSManaged var title: String
//    @NSManaged var link: String
//    @NSManaged var createdDate: Date
//}
//class CoreDataManager {
//    static let shared = CoreDataManager()
//
//    let container: NSPersistentContainer
//
//    private init() {
//        container = NSPersistentContainer(name: "MyDatabase")
//        container.loadPersistentStores { _, error in
//            if let error = error {
//                print("Failed to load Core Data stack: \(error.localizedDescription)")
//            }
//        }
//    }
//
//    func fetchHistories(from startIndex: Int, to endIndex: Int) -> [HistoryEntity] {
//        let fetchRequest: NSFetchRequest<HistoryEntity> = HistoryEntity.fetchRequest()
//        fetchRequest.fetchLimit = endIndex - startIndex + 1
//        fetchRequest.fetchOffset = startIndex
//        let sortDescriptor = NSSortDescriptor(key: "createdDate", ascending: false)
//        fetchRequest.sortDescriptors = [sortDescriptor]
//        let results = try? container.viewContext.fetch(fetchRequest)
//        return results ?? []
//    }
//
//    func fetchBookmarks(from startIndex: Int, to endIndex: Int) -> [BookmarkEntity] {
//        let fetchRequest: NSFetchRequest<BookmarkEntity> = BookmarkEntity.fetchRequest()
//        fetchRequest.fetchLimit = endIndex - startIndex + 1
//        fetchRequest.fetchOffset = startIndex
//        let sortDescriptor = NSSortDescriptor(key: "createdDate", ascending: false)
//        fetchRequest.sortDescriptors = [sortDescriptor]
//        let results = try? container.viewContext.fetch(fetchRequest)
//        return results ?? []
//    }
//
//    func insertHistory(history: LinkRecord) {
//        let viewContext = container.viewContext
//        let fetchRequest: NSFetchRequest<HistoryEntity> = HistoryEntity.fetchRequest()
//        fetchRequest.predicate = NSPredicate(format: "link == %@", history.link)
//        let results = try? viewContext.fetch(fetchRequest)
//        if results?.isEmpty ?? true {
//            let record = HistoryEntity(context: viewContext)
//            record.id = history.id
//            record.imageName = history.websiteIcon
//            record.title = history.title
//            record.link = history.link
//            record.createdDate = history.createdDate
//            try? viewContext.save()
//        }
//    }
//
//    func insertBookmark(bookmark: LinkRecord) {
//        let viewContext = container.viewContext
//        let fetchRequest: NSFetchRequest<BookmarkEntity> = BookmarkEntity.fetchRequest()
//        fetchRequest.predicate = NSPredicate(format: "link == %@", bookmark.link)
//        let results = try? viewContext.fetch(fetchRequest)
//        if results?.isEmpty ?? true {
//            let record = BookmarkEntity(context: viewContext)
//            record.id = bookmark.id
//            record.imageName = bookmark.websiteIcon
//            record.title = bookmark.title
//            record.link = bookmark.link
//            record.createdDate = bookmark.createdDate
//            try? viewContext.save()
//        }
//    }
//
//    func deleteHistories(at indexes: IndexSet) {
//        let viewContext = container.viewContext
//        let fetchRequest: NSFetchRequest<HistoryEntity> = HistoryEntity.fetchRequest()
//        fetchRequest.fetchLimit = indexes.count
//        fetchRequest.fetchOffset = indexes.first ?? 0
//        let sortDescriptor = NSSortDescriptor(key: "createdDate", ascending: false)
//        fetchRequest.sortDescriptors = [sortDescriptor]
//        let results = try? viewContext.fetch(fetchRequest)
//        results?.forEach { viewContext.delete($0) }
//        try? viewContext.save()
//    }
//
//    func deleteBookmarks(at indexes: IndexSet) {
//        let viewContext = container.viewContext
//        let fetchRequest: NSFetchRequest<BookmarkEntity> = BookmarkEntity.fetchRequest()
//        fetchRequest.fetchLimit = indexes.count
//        fetchRequest.fetchOffset = indexes.first ?? 0
//        let sortDescriptor = NSSortDescriptor(key: "createdDate", ascending: false)
//        fetchRequest.sortDescriptors = [sortDescriptor]
//        let results = try? viewContext.fetch(fetchRequest)
//        results?.forEach { viewContext.delete($0) }
//        try? viewContext.save()
//    }
//
//    func findHistory(by link: String) -> HistoryEntity? {
//        let viewContext = container.viewContext
//        let fetchRequest: NSFetchRequest<HistoryEntity> = HistoryEntity.fetchRequest()
//        fetchRequest.predicate = NSPredicate(format: "link == %@", link)
//        let results = try? viewContext.fetch(fetchRequest)
//        return results?.first
//    }
//
//    func findBookmark(by link: String) -> BookmarkEntity? {
//        let viewContext = container.viewContext
//        let fetchRequest: NSFetchRequest<BookmarkEntity> = BookmarkEntity.fetchRequest()
//        fetchRequest.predicate = NSPredicate(format: "link == %@", link)
//        let results = try? viewContext.fetch(fetchRequest)
//        return results?.first
//    }
//}
