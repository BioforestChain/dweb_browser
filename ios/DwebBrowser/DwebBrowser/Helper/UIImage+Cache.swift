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
    static func createSnapShot(withImage image: UIImage, imageName: String) -> URL {
        guard let documentsPath = NSSearchPathForDirectoriesInDomains(.documentDirectory, .userDomainMask, true).first else {
            return defaultSnapshot()
        }
        let filePath = (documentsPath as NSString).appendingPathComponent(imageName + snapshotId)
        do {
            try image.jpegData(compressionQuality: 1.0)?.write(to: URL(fileURLWithPath: filePath), options: [.atomic])
        }catch{
            print("writing image data went wrong!")
            return defaultSnapshot()
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
    
    static func defaultSnapshot()->URL{
        Bundle.main.url(forResource: "snapshot", withExtension: "png")!
    }
    
    static func defaultWebIcon()->URL{
        Bundle.main.url(forResource: "defWebIcon", withExtension: "png")!
    }
    
    static func snapshot(from localUrl: URL)->UIImage{
        do{
            return UIImage(data: try Data(contentsOf: localUrl))!
        }catch{
            return UIImage(named: "snapshot")!
        }
    }
    
}
