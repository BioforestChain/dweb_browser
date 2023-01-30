//
//  UIImage+Text.swift
//  Browser
//
//  Created by apple on 2022/8/24.
//

import Foundation
import UIKit
/**
 绘制图片
 
 @param color 背景色
 @param size 大小
 @param text 文字
 @param textAttributes 字体设置
 @param isCircular 是否圆形
 @return 图片
 */

extension UIImage{
    
    convenience init?(letters: String) {
        let style = NSMutableParagraphStyle()
        style.alignment = NSTextAlignment.center
        let sizeOfImage = CGSize(width: 175, height: 175)
        let font = UIFont(name: "SourceHanSansCN-Medium", size: 75)!
        let imageText = NSAttributedString(string: letters, attributes: [.font: font, .foregroundColor: UIColor.white, .paragraphStyle: style])
        let textHight = font.lineHeight
        let renderer = UIGraphicsImageRenderer(size: sizeOfImage)
        let image = renderer.image { context in
            UIColor(hexColor: "676C73").setFill()
            context.fill(CGRect(origin: .zero, size: sizeOfImage))
            imageText.draw(in: CGRect(origin: CGPoint(x: 0, y: (sizeOfImage.height - textHight) / 2), size: sizeOfImage))
        }
        self.init(cgImage: image.cgImage!)
    }
}

extension String {
    func image() -> UIImage? {
        let frame = CGRect(x: 0, y: 0, width: 175, height: 175)
        let nameLabel = UILabel(frame: frame)
        nameLabel.textAlignment = .center
        nameLabel.backgroundColor = UIColor(hexColor: "676C73") // UIColor(red: 30.0/255, green: 61.0/255, blue: 148.0/255, alpha: 1)
        nameLabel.textColor = .white
        let boldFont = UIFont(name: "SourceHanSansCN-Bold", size: 60)
        
        nameLabel.font = boldFont
        nameLabel.text = self
        UIGraphicsBeginImageContext(frame.size)
        if let currentContext = UIGraphicsGetCurrentContext() {
            nameLabel.layer.render(in: currentContext)
            let nameImage = UIGraphicsGetImageFromCurrentImageContext()
            return nameImage
        }
        return nil
    }
    
    
    /// Generates a `UIImage` instance from this string using a specified
    /// attributes and size.
    ///
    /// - Parameters:
    ///     - attributes: to draw this string with. Default is `nil`.
    ///     - size: of the image to return.
    /// - Returns: a `UIImage` instance from this string using a specified
    /// attributes and size, or `nil` if the operation fails.
    func image(withAttributes attributes: [NSAttributedString.Key: Any]? = nil, size: CGSize? = nil) -> UIImage? {
        let size = size ?? (self as NSString).size(withAttributes: attributes)
        
        return UIGraphicsImageRenderer(size: size).image { _ in
            (self as NSString).draw(in: CGRect(origin: .zero, size: size),
                                    withAttributes: attributes)
        }
    }
    
}

class ImageHelper: NSObject{
    
    class func saveImage(image: UIImage, name:String) -> Bool {
        guard let data = image.jpegData(compressionQuality: 1) ?? image.pngData() else {
            return false
        }
        guard let directory = try? FileManager.default.url(for: .documentDirectory, in: .userDomainMask, appropriateFor: nil, create: false) as NSURL else {
            return false
        }
        do {
            try data.write(to: directory.appendingPathComponent(name)!)
            return true
        } catch {
            print(error.localizedDescription)
            return false
        }
    }
    
    class func getSavedImage(named: String) -> UIImage? {
        if let dir = try? FileManager.default.url(for: .documentDirectory, in: .userDomainMask, appropriateFor: nil, create: false) {
            return UIImage(contentsOfFile: URL(fileURLWithPath: dir.absoluteString).appendingPathComponent(named).path)
        }
        return nil
    }
}
