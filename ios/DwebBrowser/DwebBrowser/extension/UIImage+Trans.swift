//
//  UIImage+Trans.swift
//  DwebBrowser
//
//  Created by ui06 on 6/27/23.
//

import UIKit

extension UIImage {
    static func bundleImage(name: String) -> UIImage {
        let bundle = Bundle(for: BridgeManager.self)
        let image = UIImage(named: "Resources.bundle/\(name).png", in: bundle, compatibleWith: nil)
        return image!
    }
    
    static func assetsImage(name: String) -> UIImage {
        let bundle = Bundle(for: BridgeManager.self)
        let image = UIImage(named: name, in: bundle, compatibleWith: nil)
        return image ?? UIImage(imageLiteralResourceName: "snapshot")
    }
}

