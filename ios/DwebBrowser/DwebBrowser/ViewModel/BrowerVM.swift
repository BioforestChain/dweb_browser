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
    @Published var addressBarOffset = 0.0
    
    @Published var pages = WebCacheStore.shared.store.map{Page(webWrapper: WebWrapper(webCache: $0))}
    
    @Published var sharedResources = SharedSourcesVM()
    
    
    @Published var currentSnapshotImage: UIImage? = UIImage.snapshotImage(from: URL.defaultSnapshotURL)
    {
        didSet{
            print(currentSnapshotImage)
        }
        willSet{ 
            print(newValue)

        }
    }
    
    @Published var capturedImage: UIImage?

    var cancellables = Set<AnyCancellable>()
     
    init(){
        addCapturedImageSubscriber()
    }
    
    var addressBarHeight: CGFloat{
        showingOptions ? 0:60
    }
    
    func addCapturedImageSubscriber(){
        $capturedImage
            .sink(receiveValue: {[weak self] image in
                self?.currentSnapshotImage = image
            })
//            .assign(to: \.currentSnapshotImage, on: self)
            .store(in: &cancellables)
    }
    
    func saveCaches(){
        WebCacheStore.shared.saveCaches(caches: self.pages.map({ $0.webWrapper.webCache }))

    }
    
    func removePage(at index: Int){
        var newStores = pages.map({ $0.webWrapper })
        newStores.remove(at: index)
        withAnimation(.easeIn(duration: 0.3),{
            pages = newStores.map({Page(webWrapper: $0)})
        })
        
        // TODO:
        
        // remove the snap shot
        
        //save caches to the sandbox
    }
}

