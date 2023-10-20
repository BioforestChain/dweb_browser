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
            return UIImage(named: "resource.bundle/\(name).png", in: Bundle(identifier: "resource"), compatibleWith: nil)!
        }
        return UIImage(named: "\(name)")!
    }
    
    static func assetsImage(name: String) -> UIImage {
        return .bundleImage(name: name)
    }
}

