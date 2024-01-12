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
            return UIImage(named: name, in: Bundle.browserResources, compatibleWith: nil)!
        }
        return UIImage(named: "\(name)", in: Bundle.browser, with: nil)!
    }
    
    static func assetsImage(name: String) -> UIImage {
        return bundleImage(name: name)
    }
}

