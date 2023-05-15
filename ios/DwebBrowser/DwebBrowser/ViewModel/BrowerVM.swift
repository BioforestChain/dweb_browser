//
//  BrowerVM.swift
//  DwebBrowser
//
//  Created by ui06 on 5/8/23.
//

import Foundation
import SwiftUI

class Web: ObservableObject{
    
}

class Home: ObservableObject{
    
}

class Page: Identifiable, ObservableObject, Hashable{

    
    @Published var webStore = WebViewStore( web: WebCache.createItem())

//    @Published var home: Home
    
    
    var id = UUID()
    static func == (lhs: Page, rhs: Page) -> Bool {
        return lhs.id == rhs.id
    }
    
    public func hash(into hasher: inout Hasher) {
        hasher.combine(id)
    }
}

class Address:ObservableObject{
    
}

class BrowerVM: ObservableObject {
    @Published var showingOptions = true
    @Published var selectedTabIndex = 0
    @Published var addressBarOffset = 0.0
    
    @Published var pages = [Page(),Page(),Page(),Page(),Page()]
    @Published var addresses = [Address]()
    
    @Published var sharedResources = SharedSourcesVM()
    
    
    @Published var shrinkingSnapshot: UIImage? = nil
    
    @Published var shouldTakeSnapshot = false
    
    var addressBarHeight: CGFloat{
        showingOptions ? 0:60
    }
}

