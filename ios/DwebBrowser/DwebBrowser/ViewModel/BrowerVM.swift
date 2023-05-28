//
//  BrowerVM.swift
//  DwebBrowser
//
//  Created by ui06 on 5/8/23.
//

import Foundation
import SwiftUI
import Combine

class BrowerVM: ObservableObject {
    var cancellables = Set<AnyCancellable>()
    @Published var currentSnapshotImage: UIImage
    
    @Published var selectedTabIndex = 0
    {
        didSet{
            
            currentSnapshotImage = UIImage.snapshotImage(from: WebCacheMgr.shared.store[selectedTabIndex].snapshotUrl)
        }
    }
    
    @Published var capturedImage: UIImage?{
        didSet{
            if capturedImage != nil{
//                webWrappers[selectedTabIndex].webCache.snapshotUrl = UIImage.createLocalUrl(withImage: capturedImage!, imageName: webWrappers[selectedTabIndex].webCache.id.uuidString)
//                WebCacheMgr.shared.saveCaches(caches: webWrappers.map{ $0.webCache })
            }
            
        }
    }
    
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
