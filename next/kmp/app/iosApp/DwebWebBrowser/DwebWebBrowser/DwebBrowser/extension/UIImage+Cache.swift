//
//  ImageManager.swift
//  DwebBrowser
//
//  Created by ui06 on 5/15/23.
//

import Foundation
import UIKit

private let snapshotId = "_snapshot"

// 保存页面快照到本地文件，以便下次打开app使用
extension UIImage {
    static var defaultWebIconImage = UIImage(resource: .defWebIcon)

    // 保存图片到本地文件
    static func createLocalUrl(withImage image: UIImage, imageName: String) -> URL {
        do {
            let documentsDirectory = URL.documentsDirectory
            let filePath = documentsDirectory.appendingPathComponent(imageName + snapshotId + ".jpg")
            deleteImage(with: filePath)
            try image.jpegData(compressionQuality: 1.0)?.write(to: filePath, options: .atomic)
             
            return filePath
        } catch {
            Log("Writing image data went wrong! Error: \(error)")
            return URL.defaultSnapshotURL
        }
    }

    
    // 删除缓存的图片
    static func removeImage(with fileUrl: URL) {
        let deleteFileUrls = relatedImageUrls(with: fileUrl.path)
        deleteFileUrls.forEach { deleteImage(with: $0) }
    }
    
    private static func deleteImage(with imageUrl: URL){
        let fileManager = FileManager.default
        if fileManager.fileExists(atPath: imageUrl.path) {
            do {
                try fileManager.removeItem(at: imageUrl)
            } catch {
                Log("Error while deleting the snapshot: \(error.localizedDescription)")
            }
        }
    }
    
    private static func relatedImageUrls(with filePath: String) -> [URL] {
        let suffixs = ["light_snapshot", "webtag_snapshot", "dark_snapshot"]
        guard let suffix = suffixs.filter({ filePath.contains($0) }).first else { return [] }
        let imagePath = filePath.replacingOccurrences(of: suffix, with: "")
        var urls: [URL] = []
        
        if let range = imagePath.range(of: ".jpg") {
            for i in 0...2 {
                var path = imagePath
                path.insert(contentsOf: suffixs[i], at: range.lowerBound)
                urls.append(URL(fileURLWithPath: path))
            }
            return urls
        }
        return []
    }

    static func snapshotImage(from localUrl: URL) -> UIImage {
        Log("snapshot url is \(localUrl)")
        var image: UIImage?
        do {
            image = try UIImage(data: Data(contentsOf: localUrl))!
        } catch {
            image = lightSnapshotImage
        }
        return image!
    }
}
