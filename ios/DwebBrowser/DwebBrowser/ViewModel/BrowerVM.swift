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
    @Published var showingOptions = true
    @Published var selectedTabIndex = 0
    
    @Published var pages = WebCacheStore.shared.store.map{Page(webWrapper: WebWrapper(webCache: $0))}
    
    @Published var sharedResources = SharedSourcesVM()
    
    @Published var currentSnapshotImage: UIImage?
    
    @Published var capturedImage: UIImage?
    
    var cancellables = Set<AnyCancellable>()
    
    init(){
        currentSnapshotImage = UIImage.defaultSnapShotImage
        addCapturedImageSubscriber()
    }
    
    var addressBarHeight: CGFloat{
        showingOptions ? 0 : addressBarH
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
    
    func saveCaches(){
        WebCacheStore.shared.saveCaches(caches: self.pages.map({ $0.webWrapper.webCache }))
        print("save times \(savetimes)")
        savetimes += 1
    }
    
    func removePage(at index: Int){
        var newStores = pages.map({ $0.webWrapper })
        newStores.remove(at: index)
        
        if selectedTabIndex >= newStores.count{
            selectedTabIndex = newStores.count-1
        }
        withAnimation(.easeIn(duration: 0.3),{
            pages = newStores.map{Page(webWrapper: $0)}
        })
        
        // TODO:
        
        // remove the snap shot
        
        //save caches to the sandbox
    }
}
 var savetimes = 1
