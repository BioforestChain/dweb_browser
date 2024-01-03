//
//  UIImage+Trans.swift
//  DwebBrowser
//
//  Created by ui06 on 6/27/23.
//

import UIKit

extension UIImage {
    static func bundleImage(name: String) -> UIImage {
        if ["defWebIcon","snapshot"].contains(name){
            let path = Bundle.module.path(forResource: name, ofType: "png")
            guard let path = path, let img = UIImage(contentsOfFile: path) else {
                return UIImage()
            }
            return img
        }        
        return UIImage(named: name, in: Bundle.module, with: nil)!
    }
    
    static func assetsImage(name: String) -> UIImage {
        return .bundleImage(name: name)
    }
}

