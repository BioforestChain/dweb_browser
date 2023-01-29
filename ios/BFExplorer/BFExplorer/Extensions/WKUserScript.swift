//
//  WKUserScript.swift
//  Browser
//
//  Created by apple on 2022/8/26.
//

import Foundation
import UIKit

class ComButton : UIButton{
    override init(frame: CGRect) {
        super.init(frame: frame)
        let width = frame.height * 0.8
        self.imageView?.frame = CGRect(x: frame.width - width, y: 0, width: width, height: width)
        self.titleLabel?.frame = CGRect(x: 0, y: 58, width: frame.maxX, height: frame.maxY-58)
        self.imageView?.backgroundColor = .red
        self.titleLabel?.backgroundColor = .blue
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}
