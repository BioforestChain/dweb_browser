//
//  UIImage_extension.swift
//  Plaoc-iOS
//
//  Created by ui03 on 2022/11/8.
//

import UIKit

extension UIImage {
    
    //图片转base64
    func imageToBase64() -> String {
        let imageData = self.jpegData(compressionQuality: 1.0)
        let baseString = imageData?.base64EncodedString()
        return baseString ?? ""
    }
}
