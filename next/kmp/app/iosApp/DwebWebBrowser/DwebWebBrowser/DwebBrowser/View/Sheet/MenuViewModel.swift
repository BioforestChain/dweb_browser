//
//  MenuViewModel.swift
//  DwebWebBrowser
//
//  Created by ui06 on 1/29/24.
//

import Foundation
import UIKit

@Observable
class MenuViewModel {
    var isTraceless: Bool = TracelessMode.shared.isON
    
    func addToBookmark(cache: WebCache) {
        Task(priority: .medium) {
            var imgData: Data? = try Data(contentsOf: URL.defaultWebIconURL)
            if cache.webIconUrl.isFileURL {
                imgData = try? Data(contentsOf: cache.webIconUrl)
            } else {
                let (data, response) = try await URLSession.shared.data(from: cache.webIconUrl)
                if let httpResponse = response as? HTTPURLResponse,
                   (200 ... 299).contains(httpResponse.statusCode){
                    let image = UIImage(data: data)?.resize(toSize: CGSize(width: 32, height: 32))
                    imgData = image?.pngData()
                }
            }
            browserViewDataSource.addBookmark(title: cache.title, url: cache.lastVisitedUrl.absoluteString, icon: imgData)
        }
    }
}
