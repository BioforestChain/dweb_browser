//
//  AppContentModel.swift
//  SwiftUIDemo
//
//  Created by ui03 on 2023/7/17.
//

import Foundation

struct AppContentModel: Identifiable {
    
    var id = "1"
    var icon: String = ""
    var isexpand: Bool = false
}

class OpenAppViewModel: ObservableObject {
    
    @Published var apps: [AppContentModel] = [AppContentModel()]
}


class ConfigViewModel: ObservableObject {
    
    @Published var showMenu: Bool = false
    @Published var selectedTab: String = ""
    @Published var isContract: Bool = false
}
