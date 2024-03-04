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
    
    func clear() {
        addressBar = AddressBarState()
    }
}
