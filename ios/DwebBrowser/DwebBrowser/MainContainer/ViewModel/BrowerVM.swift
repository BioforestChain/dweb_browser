//
//  BrowerVM.swift
//  DwebBrowser
//
//  Created by ui06 on 5/8/23.
//

import Foundation


class Web: ObservableObject{
    
}

class Home: ObservableObject{
    
}

class Page: ObservableObject{
//    @Published var web: Web
//    @Published var home: Home
    
}

class Address:ObservableObject{
    
}

class BrowerVM: ObservableObject {
    @Published var showingOptions = true
    @Published var selectedTabIndex = 0
    @Published var addressBarOffset = 0.0
    
    @Published var pages = [Page]()
    @Published var addresses = [Address]()
    
    
    var addressBarHeight: CGFloat{
        showingOptions ? 0:60
    }
}

