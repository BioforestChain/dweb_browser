//
//  StableForEach.swift
//  DwebBrowser
//
//  Created by ui06 on 5/25/23.
//

import SwiftUI

struct StableForEachView: View {
    @ObservedObject var browser: Browser2
    
    var body: some View {
        VStack {
            ForEach(browser.webWrappers, id: \.id) { webWrapper in
                Text(webWrapper.title)
                    .frame(width:100,height: 50)
                    .background(.red)
                    .padding()
            }
            Spacer()
            Button("Add WebWrapper") {
                let webWrapper = WebWrapper2(title: "WebWrapper \(browser.webWrappers.count)")
                withAnimation {
                    browser.add(webWrapper: webWrapper)
                }
                
            }
            Button("Remove WebWrapper") {
                
                withAnimation {
                    browser.removeOne()
                }
                
            }
            
            Button("change title"){
                browser.webWrappers[0].title = " new title"
            }
        }
    }
}

class Browser2: ObservableObject {
    @Published var webWrappers: [WebWrapper2] = []
    
    func add(webWrapper: WebWrapper2) {
        webWrappers.append(webWrapper)
    }
    func removeOne() {
        webWrappers.removeFirst()
    }
    
    
}

struct WebWrapper2: Identifiable, Equatable {
    public let id = UUID()
    var title: String
    
    static func == (lhs: WebWrapper2, rhs: WebWrapper2) -> Bool {
        return lhs.id == rhs.id && lhs.title == rhs.title
    }
}

struct StableForEach_Previews: PreviewProvider {
    static var previews: some View {
        StableForEachView(browser: Browser2())
//            .environmentObject(Browser2())
    }
}
