//
//  simplePublisher.swift
//  DwebBrowser
//
//  Created by ui06 on 5/27/23.
//

import SwiftUI

struct Person: Hashable {
    let name: String
    let age: Int
    
    func hash(into hasher: inout Hasher) {
        hasher.combine(name)
        hasher.combine(age)
    }
    
    static func == (lhs: Person, rhs: Person) -> Bool {
        return lhs.name == rhs.name && lhs.age == rhs.age
    }
}


public class SimplePublisher: ObservableObject, Identifiable, Hashable, Equatable{
    public static func == (lhs: SimplePublisher, rhs: SimplePublisher) -> Bool {
        lhs.webCache == rhs.webCache
    }
    
    public func hash(into hasher: inout Hasher) {
            hasher.combine(id)
        }
    
    public let id = UUID()
    @Published var webCache: WebCache
    init(webCache: WebCache) {
        self.webCache = webCache
    }
}



//struct Pview: View {
//    @StateObject var publisher = SimplePublisher(webCache: WebCache(title: "init"))
//    var body: some View {
//        VStack{
//            childView()
//        }
//    }
//}


//struct childView: View {
//    @ObservedObject var webCache: WebCache
//    var body: some View {
//        Text(webCache.title)
//    }
//}

class MyStruct:ObservableObject {
    @State var count = 0
}

struct InrecementView: View {
    @StateObject var myStruct = MyStruct()
    
    var body: some View {
        VStack {
            Text("Count: \(myStruct.count)")
            Button(action: {
                myStruct.count += 1
            }) {
                Text("Increment")
            }
        }
    }
}

struct simplePublisher_Previews: PreviewProvider {
    static var previews: some View {
        InrecementView()
    }
}
