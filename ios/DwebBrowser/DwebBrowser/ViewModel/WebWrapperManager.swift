//
//  WebViewVM.swift
//  DwebBrowser
//
//  Created by ui06 on 5/27/23.
//

import SwiftUI
import WebKit
import Combine

//struct IDWebWrapper: Hashable{
//    let uuid: UUID
//    let webWrapper: WebWrapper
//}

// 定义一个单例模式，用于管理所有的WKWebView对象
//let sharedWarpperStore = WebWrapperManager.shared.wrapperStore

class WebWrapperManager: ObservableObject {
    static let shared = WebWrapperManager()
    private var cancellables = Set<AnyCancellable>()

    
    @Published var wrapperStore: [WebWrapper] = []

    func webWrapper(of expectedId: UUID) -> WebWrapper{
//        if wrapperStore.count == 0 {return WebWrapper(cacheID: expectedId)}
        if let wrapper = wrapperStore.filter({ $0.id == expectedId}).first{
            return wrapper
        }else{
            let wrapper = WebWrapper(cacheID: expectedId)
            wrapperStore.append(wrapper)
            return wrapper
        }
    }

    private init() {
        WebCacheStore.shared.$store
                    .sink { [weak self] webCaches in
                        self?.wrapperStore.removeAll()
                        let cacheIds = webCaches.map{$0.id}
                        self?.wrapperStore = cacheIds.map{
                            self?.webWrapper(of: $0) ?? WebWrapper(cacheID: $0)
                        }
                    }
                    .store(in: &cancellables)
    }
    
    
    
    // TODO:
    
    // remove the snap shot
    
    //save caches to the sandbox
    
    func remove(webWrapper: WebWrapper){
        guard let wrapper = wrapperStore.firstIndex(of: webWrapper) else { return }
        withAnimation(.easeInOut(duration: 0.3),{
            wrapperStore.remove(at: wrapper)
        })
        //    if selectedTabIndex >= newStores.count{
        //        selectedTabIndex = newStores.count-1
        //    }
        
    }
}
