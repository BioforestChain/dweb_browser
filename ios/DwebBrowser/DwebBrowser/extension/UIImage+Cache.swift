//
//  ImageManager.swift
//  DwebBrowser
//
//  Created by ui06 on 5/15/23.
//

import Foundation
import UIKit

private let snapshotId = "_snapshot"

//保存页面快照到本地文件，以便下次打开app使用
extension UIImage {
    static var defaultSnapShotImage = UIImage(contentsOfFile: "\(bundlePath)/snapshot.png")!
    static var defaultWebIconImage = UIImage.assetsImage(name: "def_web_icon")

    // 保存图片到本地文件
    static func createLocalUrl(withImage image: UIImage, imageName: String) -> URL {
        let fileManager = FileManager.default
         
         do {
             let documentsDirectory = try fileManager.url(for: .documentDirectory, in: .userDomainMask, appropriateFor: nil, create: false)
             let filePath = documentsDirectory.appendingPathComponent(imageName + snapshotId + ".jpg")
             
             try image.jpegData(compressionQuality: 1.0)?.write(to: filePath, options: .atomic)
             
             return filePath
         } catch {
             print("Writing image data went wrong! Error: \(error)")
             return URL.defaultSnapshotURL
         }
    }
    
    // 删除缓存的图片
    static func removeImage(with fileUrl: URL) {
        let fileManager = FileManager.default
        
        do {
            try fileManager.removeItem(at: fileUrl)
            print("Successfully removed file at \(fileUrl)")
        } catch {
            print("Error removing file at \(fileUrl): \(error)")
        }
    }
    
    // 根据文件名读取本地图片
    static func getSnapShot(withName imageName: String) -> UIImage? {
        guard !imageName.isEmpty else {
            return nil
        }
        
        guard let documentsPath = NSSearchPathForDirectoriesInDomains(.documentDirectory, .userDomainMask, true).first else {
            return nil
        }
        
        let filePath = (documentsPath as NSString).appendingPathComponent(imageName + snapshotId)
        let image = UIImage(contentsOfFile: filePath)
        
        return image
    }

    static func snapshotImage(from localUrl: URL)->UIImage{
        print("snapshot url is \(localUrl)")
        var image: UIImage?
        do{
            image = UIImage(data: try Data(contentsOf: localUrl))!
        }catch{
            image = .bundleImage(name: "snapshot")
        }
        return image!
    }
}
