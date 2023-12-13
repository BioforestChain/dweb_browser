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
        
    func doSearch(key: String) {
        BrowserViewStateStore.shared.doSearch(key)
    }
    
    func getBrowserView() -> UIView {
        BrowserViewStateStore.shared.clear()
        return UIHostingController(rootView: BrowserView()).view!
    }
    
    func gobackIfCanDo() -> Bool {
        return BrowserViewStateStore.shared.doBackIfCan()
    }
}
