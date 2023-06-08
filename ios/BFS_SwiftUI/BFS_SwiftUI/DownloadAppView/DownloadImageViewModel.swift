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
    
    func loadIcon(urlString: String, placeHoldImage: UIImage) {
        Task {
            iconImage = await loadImage(urlString: urlString) ?? placeHoldImage
        }
    }
    
    func loadImages(imageNames: [String], placeHoldImage: UIImage) {
        Task {
            for name in imageNames {
                let image = await loadImage(urlString: name)
                if image == nil {
                    images.append(placeHoldImage)
                } else {
                    images.append(image!)
                }
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

