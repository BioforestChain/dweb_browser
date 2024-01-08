//
//  TouchThroughView.swift
//  iosApp
//
//  Created by instinct on 2023/11/27.
//  Copyright © 2023 orgName. All rights reserved.
//

import UIKit

class TouchThroughView: UIView {
        
    override func hitTest(_ point: CGPoint, with event: UIEvent?) -> UIView? {
        let views = subviews.reversed().map { $0.subviews.reversed() }.flatMap { $0 }
        for v in views {
            if let target = v.hitTest(v.convert(point, from: self), with: event) {
                return target
            }
        }
        return nil
    }
}
