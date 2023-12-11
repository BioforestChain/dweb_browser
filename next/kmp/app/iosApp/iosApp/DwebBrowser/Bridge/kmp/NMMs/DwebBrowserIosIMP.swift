//
//  DwebBrowserIosIMP.swift
//  iosApp
//
//  Created by instinct on 2023/12/7.
//  Copyright © 2023 orgName. All rights reserved.
//

import DwebShared
import UIKit
import SwiftUI

class DwebBrowserIosIMP: BrowserIosInterface {
    
    static let shared = DwebBrowserIosIMP()
    
    @Published var searchKey: String?
    
    func doSearch(key: String) {
        guard searchKey != key else { return }
        searchKey = key
    }
    
    func getBrowserView() -> UIView {
        return UIHostingController(rootView: BrowserView()).view!
    }
}
