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
    
    // 保存图片到本地文件
    static func createLocalUrl(withImage image: UIImage, imageName: String) -> URL {
        guard let documentsPath = NSSearchPathForDirectoriesInDomains(.documentDirectory, .userDomainMask, true).first else {
            return URL.defaultSnapshotURL
        }
        let filePath = (documentsPath as NSString).appendingPathComponent(imageName + snapshotId)
        do {
            try image.jpegData(compressionQuality: 1.0)?.write(to: URL(fileURLWithPath: filePath), options: [.atomic])
        }catch{
            print("writing image data went wrong!")
            return URL.defaultSnapshotURL
        }
        return URL(fileURLWithPath: filePath)
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
            image = defaultSnapShotImage
        }
        
        return image!
    }
    
    static var defaultSnapShotImage: UIImage {
        let bundle = Bundle(for: BrowserManager.self)
        let image = UIImage(named: "Resources.bundle/snapshot.png", in: bundle, compatibleWith: nil)
        return image!
    }
    
}
