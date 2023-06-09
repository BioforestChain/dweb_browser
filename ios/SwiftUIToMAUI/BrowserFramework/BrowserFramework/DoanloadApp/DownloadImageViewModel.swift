//
//  DownloadAppViewModel.swift
//  BFS_SwiftUI
//
//  Created by ui03 on 2023/5/25.
//

import SwiftUI

class DownloadImageViewModel: ObservableObject {
    
    private var cache = NSCache<AnyObject,AnyObject>()
    @Published var iconImage: UIImage?
    @Published var images: [UIImage] = []
    private var imageList: [UIImage] = []
    
    func loadIcon(urlString: String, placeHoldImageName: String) {
        
        let bundle = Bundle(for: BrowserManager.self)
        let placeHoldImage = UIImage(named: placeHoldImageName, in: bundle, compatibleWith: nil)
        Task {
            let image = await loadImage(urlString: urlString) ?? placeHoldImage
            DispatchQueue.main.async {
                self.iconImage = image
            }
        }
    }
    
    func loadImages(imageNames: [String], placeHoldImageName: String) {
        
        let bundle = Bundle(for: BrowserManager.self)
        let placeHoldImage = UIImage(named: placeHoldImageName, in: bundle, compatibleWith: nil)
        imageList.removeAll()
        Task {
            for name in imageNames {
                let image = await loadImage(urlString: name)
                if image == nil {
                    if placeHoldImage != nil {
                        imageList.append(placeHoldImage!)
                    }
                } else {
                    imageList.append(image!)
                }
            }
            DispatchQueue.main.async {
                self.images = self.imageList
            }
        }
    }
    
    private func loadImage(urlString: String) async -> UIImage? {
        
        if let image = self.cache.object(forKey: urlString as AnyObject) as? UIImage {
           return image
        }
        
        guard let url = URL(string: urlString) else { return nil }
        let resposne = try? await URLSession.shared.data(for: URLRequest(url: url))
        guard let data = resposne?.0 else { return nil }
        let image = UIImage(data: data)
        self.cache.setObject(image as AnyObject, forKey: urlString as AnyObject)
        return image
    }
}

