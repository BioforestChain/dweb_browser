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
    @Published var openingLink = OpeningLink()
    @Published var toolBarState = ToolBarState()
    @Published var webcacheStore = WebCacheStore()
    @Published var dragScale = WndDragScale()
    @Published var searchKey: String? = nil
    @Published var colorSchemeManager = ColorSchemeManager()
    @Published var selectedTabIndex = 0
    
    func clear() {
        addressBar = AddressBarState()
        openingLink = OpeningLink()
        toolBarState = ToolBarState()
        webcacheStore = WebCacheStore()
        dragScale = WndDragScale()
        searchKey = nil
        selectedTabIndex = 0
    }
}

// 这个地方暴露BrowserView的行为给外部使用
extension BrowserViewStates {
    func doBackIfCan() -> Bool {
        if addressBar.isFocused {
            addressBar.inputText = ""
            addressBar.isFocused = false
            return true
        } else if toolBarState.showMoreMenu {
            toolBarState.showMoreMenu = false
            return true
        } else if !toolBarState.shouldExpand {
            toolBarState.shouldExpand = true
            return true
        } else if toolBarState.isPresentingScanner {
            toolBarState.isPresentingScanner = false
            return true
        } else {
            guard webcacheStore.caches.count > 0 else { return false }
            let shouldShowWeb = webcacheStore.cache(at: selectedTabIndex).shouldShowWeb
            guard shouldShowWeb else { return false }
            let webwrapper = webcacheStore.webWrappers[selectedTabIndex]
            if webwrapper.webView.canGoBack {
                webwrapper.webView.goBack()
                return true
            }
        }
        return false
    }
    
    func doSearch(_ key: String?) {
        guard let key = key, !key.isEmpty, searchKey != key else {
            return
        }
        searchKey = key
    }
    
    func updateColorScheme(newScheme: Int) {
        colorSchemeManager.colorScheme = LocalColorScheme(rawValue: newScheme)!
    }
    
    func doSearchIfNeed(key: String? = nil) {
        let localKey = key != nil ? key : searchKey
        guard let key = localKey, !key.isEmpty else {
            addressBar.isFocused = false
            return
        }
        
        var deadline: CGFloat = 0.0
        if !toolBarState.shouldExpand {
            deadline = 0.5
            toolBarState.shouldExpand = true
        }
        
        if toolBarState.showMoreMenu {
            deadline = 0.5
            toolBarState.showMoreMenu = false
        }
        
        if key.isURL() {
            searchKey = nil
            addressBar.searchInputText = key
            addressBar.isFocused = false
            let url = URL.createUrl(key)
            DispatchQueue.main.asyncAfter(deadline: .now() + deadline) {
                self.openingLink.clickedLink = url
            }
        } else {
            enterType = .search
            addressBar.isFocused = true
            addressBar.searchInputText = key
        }
    }
}
