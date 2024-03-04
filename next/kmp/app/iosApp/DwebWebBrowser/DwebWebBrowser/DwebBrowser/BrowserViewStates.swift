//
//  BrowserViewStates.swift
//  iosApp
//
//  Created by instinct on 2023/12/13.
//  Copyright © 2023 orgName. All rights reserved.
//

import Foundation
import SwiftUI

class BrowserViewStates: ObservableObject {
    static let shared = BrowserViewStates()
    @Published var addressBar = AddressBarState()
    @Published var colorSchemeManager = ColorSchemeManager()
    
    func clear() {
        addressBar = AddressBarState()
    }
}

// 这个地方暴露BrowserView的行为给外部使用
extension BrowserViewStates {
    func updateColorScheme(newScheme: Int) {
        colorSchemeManager.colorScheme = LocalColorScheme(rawValue: newScheme)!
    }
}
