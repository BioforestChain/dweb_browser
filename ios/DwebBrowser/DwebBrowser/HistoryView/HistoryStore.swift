//
//  HistoryStore.swift
//  TableviewDemo
//
//  Created by ui06 on 4/4/23.
//

import Foundation
class HistoryStore: ObservableObject{
    @Published var history: [HistoryItem] = []
    
    let userdefaultKey = "userdefaultKey"
    init(){
        loadHistory()
    }
    
    func addHistoryItem(title: String, url:String, date: Date){
        let item = HistoryItem(title: title, url: url, date: date)
        history.append(item)
        saveHistory()
    }
    
    func saveHistory() {
        let data = try? JSONEncoder().encode(history)
        UserDefaults.standard.set(data, forKey: userdefaultKey)
    }
    
    
    func loadHistory() {
        if let data = UserDefaults.standard.data(forKey: userdefaultKey){
            if let items = try? JSONDecoder().decode([HistoryItem].self, from: data){
                history = items
            }
        }
        if history.count == 0 {
            history = [
                HistoryItem(title: "Google", url: "https://www.google.com", date: Date()),
                HistoryItem(title: "Apple", url: "https://www.apple.com", date: Date()),
                HistoryItem(title: "Facebook", url: "https://www.facebook.com", date: Date().addingTimeInterval(-86400)),
                HistoryItem(title: "Twitter", url: "https://www.twitter.com", date: Date().addingTimeInterval(-86400)),
                HistoryItem(title: "Amazon", url: "https://www.amazon.com", date: Date().addingTimeInterval(-172800)),
                HistoryItem(title: "Microsoft", url: "https://www.microsoft.com", date: Date().addingTimeInterval(-172800))
            ]
        }
    }
}

extension Sequence {
    func grouped <T:Comparable>(by keyPath: (Element) -> T) -> [DateGroup<T,Element>] {
        let groups = Dictionary(grouping: self, by: keyPath)
        return groups.map { DateGroup<T,Element>(date: $0.key, items: $0.value)}.sorted()
        
    }
}

struct DateGroup<T: Comparable & Hashable, Element>: Identifiable{
    let id = UUID()
    let date: T
    let items: [Element]
}

extension DateGroup: Comparable {
    static func < (lhs: DateGroup<T,Element>, rhs: DateGroup<T,Element>) -> Bool {
        return lhs.date < rhs.date
    }
    
    static func == (lhs: DateGroup<T,Element>, rhs: DateGroup<T,Element>) -> Bool {
        return lhs.date == rhs.date
    }
}

extension Date {
    func formatted(_ format: DateFormatter.Style) -> String {
        let formatter = DateFormatter()
        formatter.dateStyle = format
        return formatter.string(from: self)
    }
}
