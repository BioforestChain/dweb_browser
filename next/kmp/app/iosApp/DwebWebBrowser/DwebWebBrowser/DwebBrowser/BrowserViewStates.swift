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
    @Published var toolBarState = ToolBarState()
    @Published var searchKey: String = ""
    @Published var colorSchemeManager = ColorSchemeManager()
    
    func clear() {
        addressBar = AddressBarState()
        toolBarState = ToolBarState()
        searchKey = ""
    }
}

// 这个地方暴露BrowserView的行为给外部使用
extension BrowserViewStates {
    func doBackIfCan(isWebCanGoBack: Bool) -> Bool {
        if addressBar.isFocused {
            addressBar.inputText = ""
            addressBar.isFocused = false
            return true
        }
        
        if toolBarState.showMoreMenu {
            toolBarState.showMoreMenu = false
            return true
        }
        
        if !toolBarState.shouldExpand {
            toolBarState.shouldExpand = true
            return true
        }
        
        if toolBarState.isPresentingScanner {
            toolBarState.isPresentingScanner = false
            return true
        }
        
        if isWebCanGoBack {
            return true
        }

        return false
    }
    
    func doSearch(_ key: String) {
        guard !key.isEmpty, searchKey != key else { return }
        searchKey = key
    }
    
    func updateColorScheme(newScheme: Int) {
        colorSchemeManager.colorScheme = LocalColorScheme(rawValue: newScheme)!
    }
    
    func doSearchIfNeed(key: String = "", openNewUrl: @escaping (URL)->Void) {
        let searchText = key != "" ? key : searchKey

        guard searchText != "" else {
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
        
        if searchText.isURL() {
            searchKey = ""
            addressBar.outerSearchText = key
            addressBar.isFocused = false
            let url = URL.createUrl(searchText)
            
            DispatchQueue.main.asyncAfter(deadline: .now() + deadline) {
                openNewUrl(url)
            }
        } else {
            addressBar.outerSearchText = searchText
        }
    }
}
