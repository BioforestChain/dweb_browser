//
//  BrowerVM.swift
//  DwebBrowser
//
//  Created by ui06 on 5/8/23.
//

import Combine
import Foundation
import SwiftUI
// mike todo: import DwebShared

// mike todo:
class BrowserWebSiteInfo {
    var title: String = ""
    var url: String = ""
    var id: Int64 = 0
}

extension BrowserWebSiteInfo: Identifiable {
    
}

extension BrowserWebSiteInfo: Hashable {
    static func == (lhs: BrowserWebSiteInfo, rhs: BrowserWebSiteInfo) -> Bool {
        return lhs.title == rhs.title && lhs.url == rhs.url
    }
    
    func hash(into hasher: inout Hasher) {
         hasher.combine(title)
         hasher.combine(url)
     }
}

class DwebSharedBrowserBrowserIosService {
    
    var trackModel: Bool = false
    
    func addBookmark(title: String, url: String, icon: Data?) {
        
    }
    
    func addHistory(title: String, url: String, icon: Data?, completionHandler: (NSError?)->Void) {
        
    }
    
    func createDesktopLink(link:String, title: String, iconString:String, completionHandler: (NSError?)->Void) {
        
    }
    
    func loadBookmarks() -> [BrowserWebSiteInfo]? {
        return nil
    }
    
    func loadHistorys() -> [String: [BrowserWebSiteInfo]]? {
        return nil
    }
    
    func loadMoreHistory(off:Int32, completionHandler: (NSError?)->Void) {
        
    }
    
    func removeBookmark(bookmark:Int64) {
        
    }
    
    func removeHistory(history:Int64, completionHandler:(NSError?)->Void) {
        
    }
    
    func webSiteInfoIconToUIImage(web:BrowserWebSiteInfo) -> UIImage? {
        return nil
    }
}


// mike todo: let browserService = DwebBrowserIosSupport().browserService

let browserService = DwebSharedBrowserBrowserIosService()

class BrowserArea: ObservableObject {
    @Published var frame: CGRect = .zero
}

class WebMonitor: ObservableObject{
    @Published var loadingProgress: Double = 0{
        willSet{
            if newValue >= 1.0{
                isLoadingDone = true
            }else {
                if isLoadingDone != false{
                    isLoadingDone = false
                }
            }
        }
    }
    @Published var isLoadingDone : Bool = false
}

class SelectedTab: ObservableObject {
    @Published var curIndex: Int = 0
}

class WndDragScale: ObservableObject {
    @Published var onWidth: CGFloat = 1
    
    func properValue(floor: CGFloat, ceiling: CGFloat) -> CGFloat{
        min(ceiling, max(floor, ceiling * onWidth))
    }
    func scaledFont(maxSize: CGFloat = 18) -> Font{
        Font.system(size:  max(10, onWidth * maxSize))
    }
    func scaledFontSize(maxSize: CGFloat = 18) -> CGFloat{
        max(10, onWidth * maxSize)
    }

    var addressbarHeight: CGFloat { properValue(floor: minAddressBarH, ceiling: maxAddressBarH)}
    var toolbarHeight: CGFloat { properValue(floor: minToolBarH, ceiling: maxToolBarH)}
}

class AddressBarState: ObservableObject {
    @Published var isFocused = false
    @Published var inputText: String = ""
    @Published var shouldDisplay: Bool = true
    @Published var needRefreshOfIndex: Int = -1
    @Published var stopLoadingOfIndex: Int = -1
    @Published var searchInputText: String? = nil

}

class ToolBarState: ObservableObject {
    @Published var shouldExpand = true
//    @Published var canGoBack = false
//    @Published var canGoForward = false
//    @Published var goBackTapped = false
//    @Published var goForwardTapped = false
    @Published var createTabTapped = false
    @Published var showMoreMenu = false
    @Published var creatingDesktopLink = false
    @Published var isPresentingScanner = false
}

class ShiftAnimation: ObservableObject {
    @Published var snapshotImage: UIImage = UIImage()
    @Published var progress: AnimationProgress = .invisible
}

class TracelessMode {
    static var shared = TracelessMode()
    private let tracelessKEY = "tracelessKEY"
    var isON: Bool {
        willSet {
            browserService.trackModel = newValue
        }
    }
    
    private init() {
        isON = browserService.trackModel
    }
}

class OpeningLink: ObservableObject {
    @Published var clickedLink: URL = emptyURL
}

class TabGridState: ObservableObject {
    @Published var scale = 1.0
}

class DeleteCache: ObservableObject {
    @Published var cacheId = UUID()
}
