//
//  ImageTools.swift
//  BrowserFramework
//
//  Created by ui03 on 2023/6/9.
//

import Foundation
import UIKit

extension UIImage {
    
    static func image(for name: String) -> UIImage {
        
        let bundle = Bundle(for: BrowserManager.self)
        let image = UIImage(named: "Resources.bundle/\(name)", in: bundle, with: nil)
        return image!
    }
}
