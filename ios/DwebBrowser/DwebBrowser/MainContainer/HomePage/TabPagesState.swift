//
//  TabPagesState.swift
//  DwebBrowser
//
//  Created by ui06 on 4/28/23.
//

import SwiftUI

struct TabPageState: Identifiable, Codable{
    var id = UUID()
    var webLoadingProgress: CGFloat
    
}

@MainActor class TabPageStates: ObservableObject{
    @Published var states: [TabPageState]
    
    init() {
        states = []
    }
}
