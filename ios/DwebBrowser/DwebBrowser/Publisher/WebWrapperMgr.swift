//
//  WebViewVM.swift
//  DwebBrowser
//
//  Created by ui06 on 5/27/23.
//

import SwiftUI
import WebKit
import Combine

class WebWrapperStore: ObservableObject {
//    static let shared = WebWrapperMgr()
    private var cancellables = Set<AnyCancellable>()
    @Published var wrrapers: [WebWrapper] = []

    func webWrapper(of expectedId: UUID) -> WebWrapper{
        if let wrapper = wrrapers.filter({ $0.id == expectedId}).first{
            return wrapper
        }else{
            let wrapper = WebWrapper(cacheID: expectedId)
            wrrapers.append(wrapper)
            return wrapper
        }
    }

    init(from cacheStore: WebCacheStore){
        cacheStore.$caches
                    .sink { [weak self] webCaches in
                        print("caches titles \(webCaches.map({$0.title}))")
                        let cacheIds = webCaches.map{$0.id}
                        let newStore = cacheIds.map{
                            self?.webWrapper(of: $0) ?? WebWrapper(cacheID: $0)
                        }
                        self?.wrrapers = newStore
                    }
                    .store(in: &cancellables)
    }

//    private init() {
//        WebCacheStore.shared.$store
//                    .sink { [weak self] webCaches in
//                        print("caches titles \(webCaches.map({$0.title}))")
//                        let cacheIds = webCaches.map{$0.id}
//                        let newStore = cacheIds.map{
//                            self?.webWrapper(of: $0) ?? WebWrapper(cacheID: $0)
//                        }
//                        self?.store = newStore
//                    }
//                    .store(in: &cancellables)
//    }
}
