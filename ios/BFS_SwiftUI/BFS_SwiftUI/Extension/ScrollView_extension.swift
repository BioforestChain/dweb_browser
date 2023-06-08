//
//  ScrollView_extension.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/4/26.
//

import SwiftUI

struct ScrollViewOffsetPreferenceKey: PreferenceKey {
    
    static var defaultValue: CGFloat = 0
    
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) {
        value = nextValue()
    }
}
