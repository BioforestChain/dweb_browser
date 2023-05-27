//
//  BrowerVM.swift
//  DwebBrowser
//
//  Created by ui06 on 5/8/23.
//

import Foundation
import SwiftUI
import Combine

class Web: ObservableObject{
    
}

class Home: ObservableObject{
    
}

class Page: Identifiable, ObservableObject, Hashable{
    var id = UUID()
    
    @Published var webWrapper: WebWrapper
    
    init(id: UUID = UUID(), webWrapper: WebWrapper) {
        self.id = id
        self.webWrapper = webWrapper
    }
    
    static func == (lhs: Page, rhs: Page) -> Bool {
        lhs.id == rhs.id
    }
    
    public func hash(into hasher: inout Hasher) {
        hasher.combine(id)
    }
}

class BrowerVM: ObservableObject {
    
//    @Published var showingOptions = true
    @Published var selectedTabIndex = 0
    {
        didSet{
            currentSnapshotImage = UIImage.snapshotImage(from: WebCacheStore.shared.store[selectedTabIndex].snapshotUrl)
        }
    }
    
//    @Published var webWrappers = WebCacheStore.shared.store.map{WebWrapper(webCache: $0)}
    
    @Published var sharedResources = SharedSourcesVM()
    
    @Published var currentSnapshotImage: UIImage
    
    @Published var capturedImage: UIImage?{
        didSet{
            if capturedImage != nil{
//                webWrappers[selectedTabIndex].webCache.snapshotUrl = UIImage.createLocalUrl(withImage: capturedImage!, imageName: webWrappers[selectedTabIndex].webCache.id.uuidString)
//                WebCacheStore.shared.saveCaches(caches: webWrappers.map{ $0.webCache })
            }
            
        }
    }
    
//    var webWrappers: [WebWrapper]{
//        pages.map({ $0.webWrapper})
//    }
    
    var cancellables = Set<AnyCancellable>()
    
    init(){
        currentSnapshotImage = UIImage.defaultSnapShotImage
        addCapturedImageSubscriber()
    }
    
    func addCapturedImageSubscriber(){
        $capturedImage
            .sink(receiveValue: {[weak self] image in
                if let image = image {
                    self?.currentSnapshotImage = image
                }
            })
            .store(in: &cancellables)
    }
}

 var savetimes = 1
